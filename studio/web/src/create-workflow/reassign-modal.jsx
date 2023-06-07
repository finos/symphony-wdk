import { atoms } from '../core/atoms';
import {
    Button, Modal, ModalTitle, ModalBody, ModalFooter, Dropdown,
} from "@symphony-ui/uitoolkit-components/components";
import { useState } from 'react';
import { useRecoilState } from 'recoil';
import api from '../core/api';
import { editor } from 'monaco-editor/esm/vs/editor/editor.api';

const debounce = (fn, time) => {
    let timeoutId;
    return (...args) => {
        if (timeoutId) {
            clearTimeout(timeoutId);
        }
        timeoutId = setTimeout(() => {
            timeoutId = null;
            fn(...args);
        }, time);
    };
};

const ReassignModal = ({ setShow }) => {
    const { addWorkflow, searchUser, showStatus } = api();
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const [ newOwner, setNewOwner ] = useState();
    const setIsContentChanged = useRecoilState(atoms.isContentChanged)[1];
    const setContents = useRecoilState(atoms.contents)[1];
    const setAuthor = useRecoilState(atoms.author)[1];

    const submitReassign = () => {
        setLoading(true);
        const swadl = editor.getModels()[0].getValue();
        const createdBy = newOwner.value;
        const description = 'Owner changed';
        addWorkflow({ swadl, createdBy, description }).then(() => {
            setLoading(false);
            setShow(false);
            setIsContentChanged('original');
            setAuthor(createdBy);
            setContents(swadl);
            showStatus(false, 'Workflow owner reassigned');
        }, ({ message }) => showStatus(true, message));
    };

    const searchPeople = (input) => new Promise((resolve) => searchActual(input, resolve));
    const searchActual = debounce((input, resolve) => {
        const term = input.trim();
        if (term.length < 2) {
            resolve([]);
        }
        searchUser(term).then(data => resolve(
            data.map(({ id, displayName }) => ({ label: displayName, value: id }))
        ));
    }, 300);

    return (
        <Modal size="medium" show closeButton onClose={() => setShow(false)}>
            <ModalTitle>Reassign Workflow Owner</ModalTitle>
            <ModalBody style={{ minHeight: '17rem' }}>
                <Dropdown
                    blurInputOnSelect
                    label="Select the new owner"
                    defaultOptions={false}
                    isInputClearable
                    noOptionMessage="No results available"
                    asyncOptions={searchPeople}
                    onChange={({ target }) => setNewOwner(target.value)}
                    value={newOwner}
                    isDisabled={loading}
                />
            </ModalBody>
            <ModalFooter>
                <Button
                    variant="primary"
                    onClick={submitReassign}
                    disabled={loading || !newOwner}
                    loading={loading}
                >
                    Reassign
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
export default ReassignModal;
