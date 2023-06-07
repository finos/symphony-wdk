import { useEffect, useState, useRef } from 'react';
import {
    Button, Icon, Loader, TextField, Modal, ModalTitle, ModalBody, ModalFooter,
} from "@symphony-ui/uitoolkit-components/components";
import TemplateSelector from './template-selector';
import api from '../core/api';
import { atoms } from '../core/atoms';
import { useRecoilState } from 'recoil';

const CreateWorkflowModal = ({ setShow }) => {
    const [ loading, setLoading ] = useState(false);
    const [ workflows, setWorkflows ] = useRecoilState(atoms.workflows);
    const setCurrentWorkflow = useRecoilState(atoms.currentWorkflow)[1];
    const setActiveVersion  = useRecoilState(atoms.activeVersion)[1];
    const [ newName, setNewName ] = useState('');
    const [ swadlTemplate, setSwadlTemplate ] = useState();
    const [ pageLoading, setPageLoading ] = useState(false);
    const [ templateLoading, setTemplateLoading ] = useState(false);
    const { addWorkflow, listWorkflows, showStatus } = api();
    const session = useRecoilState(atoms.session)[0];

    const showToast = (error, msg) => {
        showStatus(error, msg);
        setLoading(false);
    };

    const createWorkflow = () => {
        if (newName.trim().length < 3) {
            showToast(true, 'Workflow name needs to be at least 3 characters long');
            return;
        }
        if (newName.trim().indexOf(' ') > -1) {
            showToast(true, 'Workflow name cannot contain spaces');
            return;
        }
        const newId = newName.trim().toLowerCase();
        if (workflows.map(w => w.value).indexOf(newId) > -1) {
            showToast(true, 'Another workflow with this ID already exists');
            return;
        }
        setLoading(true);
        const template = swadlTemplate
            .replace(/newId/g, newId)
            .replace(/id: ([\w\-]+)/, `id: ${newId}`);

        addWorkflow({ swadl: template, createdBy: session.id, description: "New workflow" }).then(
            () => listWorkflows((response) => {
                showToast(false, 'New workflow added');
                setShow(false);
                setNewName('');
                const values = response
                    .map(({ id, version }) => ({ label: id, value: id, version }))
                    .sort((a, b) => a.label > b.label ? 1 : -1);
                setWorkflows(values);
                const newWorkflow = values.find(w => w.value === newId);
                setCurrentWorkflow(newWorkflow);
                setActiveVersion(newWorkflow.version);
            }),
            () => setLoading(false)
        );
    };

    const nameRef = useRef();
    useEffect(() => {
        if (nameRef.current) {
            nameRef?.current?.focus();
        }
    }, []);

    return (
        <Modal size="large" show>
            <ModalTitle>Create Workflow</ModalTitle>
            <ModalBody style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <TextField
                    ref={nameRef}
                    label="Name"
                    showRequired={true}
                    value={newName}
                    disabled={loading}
                    onChange={({ target }) => setNewName(target.value)}
                />
                <TemplateSelector {...{ setSwadlTemplate, pageLoading, setPageLoading, templateLoading, setTemplateLoading }} />
            </ModalBody>
            <ModalFooter>
                <Button
                    onClick={createWorkflow}
                    loading={loading}
                    disabled={newName==='' || loading || pageLoading || templateLoading}
                >
                    Create
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setShow(false)}
                    disabled={loading || pageLoading || templateLoading}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
};

const CreateWorkflowButton = () => {
    const [ show, setShow ] = useState(false);
    const editMode = useRecoilState(atoms.editMode)[0];
    const isContentChanged = useRecoilState(atoms.isContentChanged)[0];

    return (
        <>
            <Button
                variant="primary"
                disabled={!editMode || isContentChanged === 'modified'}
                onClick={() => setShow(true)}
                iconLeft={<Icon iconName="plus" />}
            >
                Workflow
            </Button>
            { show && <CreateWorkflowModal setShow={setShow} /> }
        </>
    );
};

export default CreateWorkflowButton;
