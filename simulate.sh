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
  echo "./simulate.sh -w {NB_WORKFLOWS_TO_DEPLOY} -p {NB_PROCESSES_PER_WORKFLOW} -a {NB_ACTIVITIES_PER_WORKFLOW}"
  exit
fi

if [[ NB_WORKFLOWS -lt 2 ]] || [[ $NB_PROC_PER_WORKFLOW -lt 2 ]] || [[ $NB_ACTIVITIES_PER_WORKFLOW -lt 3 ]]
then
  echo "{NB_WORKFLOWS} and {NB_PROC_PER_WORKFLOW} should be greater than 1, {NB_ACTIVITIES_PER_WORKFLOW} should be greater than 2"
  exit
fi

#echo "NB_WORKFLOWS: $NB_WORKFLOWS";
#echo "NB_PROC_PER_WORKFLOW: $NB_PROC_PER_WORKFLOW";

swadl="
    id: %WORKFLOW_ID_PLACEHOLDER%
activities:
      - send-message:
          id: ping
          on:
            request-received:
              token: mytoken
          content: Pong-initialActivity-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%
          to:
            stream-id: \${event.args.streamId}

      - send-message:
          id: ping0
          on:
            message-received:
              content: /continue-activity-0-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%
          content: continue

    "
for (( i = 1; i < $NB_ACTIVITIES_PER_WORKFLOW; i++ )); do
  activity="
      - send-message:
          id: ping${i}
          on:
            message-received:
              content: /continue-actvity-${i}-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%
          content: Pong-activity-${i}-workflow-%WORKFLOW_NUMBER_PLACEHOLDER%"
  swadl="${swadl} ${activity}"
done

#swadl=${swadl//NB/.}

# DEPLOYMENT
for (( i = 0; i < $NB_WORKFLOWS; i++ )); do
    WID="simulation-workflow-${i}"
    swadlUpdated=${swadl//%WORKFLOW_ID_PLACEHOLDER%/${WID}}
    swadlUpdated=${swadlUpdated//%WORKFLOW_NUMBER_PLACEHOLDER%/${i}}

    echo $swadlUpdated

#    swadl="
#    id: ${WID}
#activities:
#      - send-message:
#          id: ping
#          on:
#            request-received:
#              token: mytoken
#          content: Pong${i}
#          to:
#            stream-id: \${event.args.streamId}

#      - send-message:
#          id: ping2
#          on:
#            message-received:
#              content: /continue${i}
#          content: continue
#    "
    #echo $swadl
    #curl --location -s -w 'Total: %{time_total}s\n' --request POST 'http://localhost:8080/wdk/v1/management/workflows
    deploymentDuration=$(curl --location -s --request POST 'http://localhost:8080/wdk/v1/management/workflows' \
                              --header 'X-Management-Token: myBestToken' \
                              --header 'Content-Type: multipart/form-data' \
                              --form 'swadl'="$swadlUpdated" \
                              --form 'description="bla"')
    echo $deploymentDuration
done




exit
# PROCESSES EXECUTION
for (( i = 0; i < $NB_WORKFLOWS; i++ )); do
  WID="simulation-workflow-${i}"
  for (( j = 0; j < $NB_PROC_PER_WORKFLOW; j++ )); do
    echo ${i} ':' ${j}
    curl --location --request POST 'http://localhost:8080/wdk/v1/workflows/'${WID}'/execute' \
          --header 'X-Workflow-Token: mytoken' \
          --header 'Content-Type: application/json' \
          --data-raw '{"args": {"streamId": "PZcH8kBHQ1XWMGWYx5FUvX///no6GiJJdA=="}}'
    echo ${i} '_' ${j}
    sleep 5
  done
done
