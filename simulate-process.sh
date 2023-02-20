while getopts w:p:a: flag
do
    case "${flag}" in
        w) NB_WORKFLOWS=${OPTARG};;
        p) NB_PROC_PER_WORKFLOW=${OPTARG};;
        a) NB_ACTIVITIES_PER_WORKFLOW=${OPTARG};;
    esac
done

if [ -z "$NB_WORKFLOWS" ] || [ -z "$NB_PROC_PER_WORKFLOW" ] || [ -z "$NB_ACTIVITIES_PER_WORKFLOW" ]
then
  echo "WRONG INPUT. THE EXPECTED FORMAT IS:"
  echo "./simulate-process.sh -w {NB_WORKFLOWS_TO_DEPLOY} -p {NB_PROCESSES_PER_WORKFLOW} -a {NB_ACTIVITIES_PER_WORKFLOW}"
  exit
fi

#if [[ NB_WORKFLOWS -lt 2 ]] || [[ $NB_PROC_PER_WORKFLOW -lt 2 ]] || [[ $NB_ACTIVITIES_PER_WORKFLOW -lt 3 ]]
if [[ NB_WORKFLOWS -lt 0 ]] || [[ $NB_PROC_PER_WORKFLOW -lt 0 ]] || [[ $NB_ACTIVITIES_PER_WORKFLOW -lt 3 ]]
then
  echo "{NB_WORKFLOWS} and {NB_PROC_PER_WORKFLOW} should be greater than 1, {NB_ACTIVITIES_PER_WORKFLOW} should be greater than 2"
  exit
fi

#echo "NB_WORKFLOWS: $NB_WORKFLOWS";
#echo "NB_PROC_PER_WORKFLOW: $NB_PROC_PER_WORKFLOW";


wc -c data/process_engine.mv.db  | awk '{print $1}'

swadl="
    id: %WORKFLOW_ID_PLACEHOLDER%
activities:
      - send-message:
          id: ping
          on:
            request-received:
              id: requestevent
              token:
          content: Pong-initialActivity-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%
          to:
            stream-id: \${event.args.streamId}

      - send-message:
          id: ping0
          on:
            timer-fired:
              at: %TIMER_FIRED_PLACEHOLDER%
          to:
            stream-id: \${requestevent.args.streamId}
          content: Pong-activity-0-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%

    "
for (( i = 1; i < $NB_ACTIVITIES_PER_WORKFLOW + 1; i++ )); do
  activity="
      - send-message:
          id: ping${i}
          to:
            stream-id: \${event.args.streamId}
          content: Pong-activity-${i}-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%"
  swadl="${swadl} ${activity}"
done

# DEPLOYMENT
echo "Start deployments"
TIMER_FIRED=$(date -v+180M -v-1H +%Y-%m-%dT-%H:%M:%S)
echo "TIME_FIRED = " $TIMER_FIRED
for (( i = 0; i < $NB_WORKFLOWS; i++ )); do
    WID="simulation-workflow-${i}"
    swadlUpdated=${swadl//%WORKFLOW_ID_PLACEHOLDER%/${WID}}
    swadlUpdated=${swadlUpdated//%WORKFLOW_NUMBER_PLACEHOLDER%/${i}}
    swadlUpdated=${swadlUpdated//%TIMER_FIRED_PLACEHOLDER%/${TIMER_FIRED}}


    #curl --location -s -w 'Total: %{time_total}s\n' --request POST 'http://localhost:8080/wdk/v1/management/workflows
    deploymentDuration=$(curl --location -s --request POST 'http://localhost:8080/wdk/v1/management/workflows' \
                              --header 'X-Management-Token: ' \
                              --header 'Content-Type: multipart/form-data' \
                              --form 'swadl'="$swadlUpdated" \
                              --form 'description="bla"')
    echo $deploymentDuration
done

# PROCESSES EXECUTION
echo "Start creating processes"
for (( i = 0; i < $NB_WORKFLOWS; i++ )); do
  WID="simulation-workflow-${i}"
  for (( j = 0; j < $NB_PROC_PER_WORKFLOW; j++ )); do
    curl --location --request POST 'http://localhost:8080/wdk/v1/workflows/'${WID}'/execute' \
          --header 'X-Workflow-Token: ' \
          --header 'Content-Type: application/json' \
          --data-raw '{"args": {"streamId": "rcCFh4HrsdHjh8gC59etlX///no6GZh/dA=="}}'
    echo $i"/"$NB_WORKFLOWS "-" $j"/"$NB_PROC_PER_WORKFLOW
    sleep 2
  done
done

TIMER_FIRED=$(date +%Y-%m-%dT-%H:%M:%S)
echo "TIME_FIRED = " $TIMER_FIRED

# the db is not updated immediately, we need to wait for a while before checking its size
#sleep 10
#wc -c data/process_engine.mv.db  | awk '{print $1}'
