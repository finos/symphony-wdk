import { atoms } from '../core/atoms';
import { Button } from '@symphony-ui/uitoolkit-components/components';
import { DetailPlane, TableTitle, Table, Row } from './styles';
import { useRecoilState } from 'recoil';
import { useState, useEffect } from 'react';
import api from '../core/api';
import Spinner from '../core/spinner';

const ActivityList = ({ selectedInstance, setInspectorPayload }) => {
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const [ activities, setActivities ] = useState();
    const regexType = /(.*)(_)/gm;
    const { listWorkflowInstanceActivities } = api();

    useEffect(() => {
        if (selectedInstance) {
            listWorkflowInstanceActivities(currentWorkflow.value, selectedInstance.instanceId,
                (response) => setActivities(response));
        }
    }, [ selectedInstance ]);

    const Content = () => (
        <Table>
            <thead>
                <Row>
                    <th>ID</th>
                    <th>Type</th>
                    <th>Start</th>
                    <th>End</th>
                    <th>Outputs</th>
                </Row>
            </thead>
            <tbody>
                {activities.nodes.map((row, i) => (
                    <Row key={i}>
                        <td>{row.nodeId.replace(regexType, '')}</td>
                        <td>{row.type}</td>
                        <td>{(new Date(row.startDate)).toLocaleString()}</td>
                        <td>{row.endDate ? (new Date(row.endDate)).toLocaleString() : ""}</td>
                        <td>
                            { Object.keys(row.outputs).length > 0 && (
                                <Button
                                    className="inspect"
                                    size="small"
                                    variant="secondary"
                                    onClick={() => setInspectorPayload(row.outputs) }
                                >
                                    Inspect
                                </Button>
                            )}
                        </td>
                    </Row>
                ))}
            </tbody>
        </Table>
    );

    return (
        <>
            <TableTitle>Activities</TableTitle>
            <DetailPlane>
                { !activities ? <Spinner /> : <Content /> }
            </DetailPlane>
        </>
    );
};
export default ActivityList;
