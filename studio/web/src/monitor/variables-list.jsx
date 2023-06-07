import { atoms } from '../core/atoms';
import { Button } from '@symphony-ui/uitoolkit-components/components';
import { DetailPlane, TableTitle, Table } from './styles';
import { useRecoilState } from 'recoil';
import { useState, useEffect } from 'react';
import api from '../core/api';
import Spinner from '../core/spinner';

const VariablesList = ({ selectedInstance, setInspectorPayload }) => {
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const [ variables, setVariables ] = useState();
    const { listWorkflowInstanceVariables } = api();

    useEffect(() => {
        if (selectedInstance) {
            listWorkflowInstanceVariables(currentWorkflow.value, selectedInstance.instanceId,
                (response) => setVariables(response));
        }
    }, [ selectedInstance ]);

    const InspectButton = ({ payload }) => (
        <Button
            className="inspect"
            size="small"
            variant="secondary"
            onClick={() => setInspectorPayload(payload) }
        >
            Inspect
        </Button>
    );

    const formatVariable = (variable) => !variable ? 'null'
        : (typeof variable === 'object') ? <InspectButton payload={variable} />
        : variable.toString();

    const Content = () => (
        <Table>
            <thead>
                <tr>
                    <th>Updated</th>
                    <th>Variable</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
                { variables.map(({ outputs, updateTime }) =>
                    Object.keys(outputs).map((key, i) => (
                        <tr key={i}>
                            <td>{(new Date(updateTime)).toLocaleString()}</td>
                            <td>{key}</td>
                            <td>{formatVariable(outputs[key])}</td>
                        </tr>
                    ))
                )}
            </tbody>
        </Table>
    );

    return (
        <>
            <TableTitle>Variables</TableTitle>
            <DetailPlane>
                { !variables ? <Spinner /> : <Content /> }
            </DetailPlane>
        </>
    );
};
export default VariablesList;
