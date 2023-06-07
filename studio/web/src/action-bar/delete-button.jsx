import { atoms } from '../core/atoms';
import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle,
} from '@symphony-ui/uitoolkit-components/components';
import { useRecoilState } from 'recoil';
import { useState } from 'react';
import api from '../core/api';

const DeleteButton = () => {
    const editMode = useRecoilState(atoms.editMode)[0];
    const session = useRecoilState(atoms.session)[0];
    const author = useRecoilState(atoms.author)[0];
    const [ show, setShow ]  = useState(false);
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];

    const ConfirmDeleteModal = () => {
        const setWorkflows = useRecoilState(atoms.workflows)[1];
        const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
        const { deleteWorkflow, showStatus } = api();

        const submitDeleteWorkflow = () => {
            setLoading(true);
            deleteWorkflow(currentWorkflow.value, () => {
                setLoading(false);
                setShow(false);
                showStatus(false, 'Workflow deleted');
                setWorkflows((old) => old.filter((w) => w.value !== currentWorkflow.value));
            });
        };

        return (
            <Modal size="medium" show={show}>
                <ModalTitle>Confirm Delete</ModalTitle>
                <ModalBody>This will delete the workflow permanently. Are you sure?</ModalBody>
                <ModalFooter>
                    <Button
                        variant="primary-destructive"
                        onClick={submitDeleteWorkflow}
                        disabled={loading}
                        loading={loading}
                    >
                        Delete
                    </Button>
                    <Button
                        variant="secondary"
                        onClick={() => setShow(false)}
                        disabled={loading}
                    >
                        Cancel
                    </Button>
                </ModalFooter>
            </Modal>
        );
    };

    return (
        <>
            <Button
                style={{ display: 'block'}}
                variant="primary-destructive"
                disabled={!currentWorkflow || !editMode || author !== session.id}
                onClick={() => setShow(true)}
            >
                Delete
            </Button>
            <ConfirmDeleteModal />
        </>
    );
};
export default DeleteButton;
