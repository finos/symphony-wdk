More details: 
- PoC using Camunda: https://perzoinc.atlassian.net/wiki/spaces/DevX/pages/1567034442/PLAT-10312+Workflow+API+PoC+using+Camunda
- Definition of language v1: https://perzoinc.atlassian.net/wiki/spaces/DevX/pages/2118483969/PLAT-10316+Define+Workflow+language+v1

Usage:
- To run a workflow, send a message with bpmn file as attachment.
- To validate a YAML file describing the workflow and generate the corresponding bpmn file, send "/validate" with YAML file as attachment. The bpmn file will be generated at build/resources/main/output.bpmn.
