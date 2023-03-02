import requests
import json
import csv
import sys
from datetime import datetime

'''Usage:
This script is used to analyze WDK processes after a simulation. It uses the monitoring api for this purpose.
Given the configuration of the executed simulation {NB_WORKFLOWS_DEPLOYED} {NB_PROCESSES_PER_WORKFLOW} {NB_ACTIVITIES_PER_WORKFLOW},
it will call the monitoring api, update JSON files with the new data and generates CSV files to present this data.

Requirements:
- Install `requests` package: pip install requests
- WDK app to be running (on localhost for the moment. To be improved to make it configurable.)
- For a given configuration, 2 JSON files needs to be created beforehand:  (To be improved to create them if they don't exist)
states/states_{NB_WORKFLOWS_DEPLOYED}_{NB_ACTIVITIES_PER_WORKFLOW}.json
duration/duration_{NB_WORKFLOWS_DEPLOYED}_{NB_ACTIVITIES_PER_WORKFLOW}.json

'''


########################## Global variables ##########################
HEADERS = {
  'X-Monitoring-Token': 'myBestToken',
  'Content-Type': 'application/json'
}

# Script arguments handling
arguments_length = len(sys.argv)
if (arguments_length < 4):
    raise ValueError("WRONG INPUT. THE EXPECTED FORMAT IS: python script.py {NB_WORKFLOWS_DEPLOYED} {NB_PROCESSES_PER_WORKFLOW} {NB_ACTIVITIES_PER_WORKFLOW}")

NB_WORKFLOWS_DEPLOYED = int(sys.argv[1])
NB_PROCESSES_PER_WORKFLOW = int(sys.argv[2])
NB_ACTIVITIES_PER_WORKFLOW = int(sys.argv[3])

if NB_WORKFLOWS_DEPLOYED < 1 or NB_PROCESSES_PER_WORKFLOW < 1 or NB_ACTIVITIES_PER_WORKFLOW < 2:
    raise ValueError("{NB_WORKFLOWS_DEPLOYED} and {NB_PROCESSES_PER_WORKFLOW} should be greater than 1, {NB_ACTIVITIES_PER_WORKFLOW} should be greater than 2")


########################## Functions ##########################
def compute_process_actual_duration_average(all_deployed_workflows_map, nb_running_processes):
    '''
        Computes the actual duration of a process, for the configured simulation.
        Actual duration is the duration of the process minus the time elapsed between the end time of the first activity
        and the start time of the second activity.
        The first activity is triggered with a request while the second one is triggered with a timer fired event.
        Given that the timer fired is the same for all processes, but the processes are started sequentially,
        then that time elapsed between 1st and 2nd activity is not the same for all processes. Also, this elapsed time
        is due to the fact of waiting for the timer to be fired and not impacted by WDK performance, so it is not meant
        to be considered in the actual process duration average.
    '''

    average_actual_process_duration = 0
    #average_processes_duration = 0
    for workflow in all_deployed_workflows_map:
        for instance in all_deployed_workflows_map[workflow]:
            duration = all_deployed_workflows_map[workflow][instance]
            print(workflow + ", " + instance + ", " + duration)

            url = "http://localhost:8080/wdk/v1/workflows/" + workflow + "/instances/" + str(instance) + "/states"
            response = requests.request("GET", url, headers=HEADERS, data={})

            nodes = json.loads(response.text)['nodes']

            first_activity_end_date = nodes[0]['endDate']
            timer_fired_event_start_date = nodes[1]['startDate']

            # convert string to datetime
            try:
                dt1 = datetime.strptime(first_activity_end_date, "%Y-%m-%dT%H:%M:%S.%fZ")
            except ValueError:
                dt1 = datetime.strptime(first_activity_end_date, "%Y-%m-%dT%H:%M:%S%fZ")

            try:
                dt2 = datetime.strptime(timer_fired_event_start_date, "%Y-%m-%dT%H:%M:%S.%fZ")
            except ValueError:
                dt2 = datetime.strptime(timer_fired_event_start_date, "%Y-%m-%dT%H:%M:%S%fZ")
            # difference between datetime in time delta
            # time elapsed between the moment the process is started, and the moment the timer got fired for the 2nd activity
            delta = dt2 - dt1
            delta_seconds = delta.total_seconds()

            duration = duration.split('PT')[1]
            if 'H' in duration:
                hours = duration.split('H')[0]
                duration = duration.split('H')[1]
            else:
                hours = 0

            if 'M' in duration:
                minutes = duration.split('M')[0]
                seconds = duration.split('M')[1].split('S')[0]
            else:
                minutes = 0
                seconds = duration.split('S')[0]

            if not seconds:
                seconds = 0
            seconds_float = float(seconds)
            minutes_float = float (minutes)
            hours_float = float (hours)
            duration_seconds_float= seconds_float + (minutes_float * 60) + (hours_float * 3600)
            activities_duration = duration_seconds_float - delta_seconds

            #average_processes_duration += duration_seconds_float
            average_actual_process_duration += activities_duration
    return average_actual_process_duration/nb_running_processes


def update_process_actual_duration_average_outputs_file(average_actual_process_duration):
    '''
        Updates json output file for processes average duration
    '''

    json_data_filename = "duration/duration_" + str(NB_WORKFLOWS_DEPLOYED) + "_" + str(NB_ACTIVITIES_PER_WORKFLOW) + ".json"
    with open(json_data_filename, "r") as json_file:
        duration_json_data = json.load(json_file)
    json_file.close()

    index = -1
    for entry in duration_json_data:
        if entry['nb_workflows'] == NB_WORKFLOWS_DEPLOYED:
            found = True
            index = duration_json_data.index(entry)

    key = 'av_' + str(NB_PROCESSES_PER_WORKFLOW)

    if index == -1:
        duration_json_data.append({'nb_workflows': NB_WORKFLOWS_DEPLOYED, key: average_actual_process_duration})
    else:
        duration_json_data[index][key] = average_actual_process_duration


    # Update JSON outputs file
    with open(json_data_filename, "w") as json_file:
        json.dump(duration_json_data, json_file)
    json_file.close()

    return duration_json_data


def update_processes_states_outputs_file(nb_completed_processes, nb_pending_processes, nb_failed_processes):
    '''
        Updates json output file for processes states
    '''

    # Read JSON outputs file
    json_data_filename = "states/states_" + str(NB_WORKFLOWS_DEPLOYED) + "_" + str(NB_ACTIVITIES_PER_WORKFLOW) + ".json"
    with open(json_data_filename, "r") as json_file:
        json_data = json.load(json_file)
    json_file.close()

    index = -1
    for entry in json_data:
        if entry['nb_processes'] == NB_PROCESSES_PER_WORKFLOW:
            found = True
            index = json_data.index(entry)

    if index == -1:
        json_data.append({'nb_processes': NB_PROCESSES_PER_WORKFLOW, 'completed': nb_completed_processes, 'failed': nb_pending_processes + nb_failed_processes})
    else:
        json_data[index]['nb_processes'] = NB_PROCESSES_PER_WORKFLOW
        json_data[index]['completed'] = nb_completed_processes
        json_data[index]['failed'] = nb_pending_processes + nb_failed_processes

    # Update JSON outputs file
    with open(json_data_filename, "w") as json_file:
        json.dump(json_data, json_file)
    json_file.close()

    return json_data


def update_csv_outputs_file(outputs_csv_filename, json_data):
    '''
        Create CSV outputs file from json_data object
    '''
    data_file = open(outputs_csv_filename, 'w', newline='')
    csv_writer = csv.writer(data_file)
    count = 0
    for data in json_data:
        if count == 0:
            header = data.keys()
            csv_writer.writerow(header)
            count += 1
        csv_writer.writerow(data.values())

    data_file.close()


########################## Main script ##########################
nb_running_processes = 0

# Call monitoring api
nb_failed_processes = 0
nb_pending_processes = 0
nb_completed_processes = 0
all_deployed_workflows_map = {}
for i in range(NB_WORKFLOWS_DEPLOYED):
    workflow_id = "simulation-xxxx-workflow-" + str(i)

    # Check failing processes
    url = "http://localhost:8080/wdk/v1/workflows/" + workflow_id + "/instances?status=failed"
    response_json_data = json.loads(requests.request("GET", url, headers=HEADERS, data={}).text)
    nb_failed_processes += len(response_json_data)

    # Check pending processes, they are also considered as failing as they should have been completed
    url = "http://localhost:8080/wdk/v1/workflows/" + workflow_id + "/instances?status=pending"
    response_json_data = json.loads(requests.request("GET", url, headers=HEADERS, data={}).text)
    nb_pending_processes += len(response_json_data)

    # Check completed processes
    url = "http://localhost:8080/wdk/v1/workflows/" + workflow_id + "/instances?status=completed"
    response = requests.request("GET", url, headers=HEADERS, data={})
    workflow_map = {}
    for j in json.loads(response.text):
        workflow_map[j['instanceId']] = j['duration']
        nb_running_processes += 1
        nb_completed_processes += 1
    all_deployed_workflows_map[workflow_id] = workflow_map


# Output processes states
states_json_data = update_processes_states_outputs_file(nb_completed_processes, nb_pending_processes, nb_failed_processes)
outputs_csv_filename = "states/states_" + str(NB_WORKFLOWS_DEPLOYED) + "_" + str(NB_ACTIVITIES_PER_WORKFLOW) + ".csv"
update_csv_outputs_file(outputs_csv_filename, states_json_data)

# Outputs processes durations
average_actual_process_duration = compute_process_actual_duration_average(all_deployed_workflows_map, nb_running_processes)
duration_json_data = update_process_actual_duration_average_outputs_file(average_actual_process_duration)
outputs_csv_filename = "duration/duration_" + str(NB_WORKFLOWS_DEPLOYED) + "_" + str(NB_ACTIVITIES_PER_WORKFLOW) + ".csv"
update_csv_outputs_file(outputs_csv_filename, duration_json_data)
