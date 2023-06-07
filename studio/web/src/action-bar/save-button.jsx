import { atoms } from '../core/atoms';
import { useRecoilState } from 'recoil';
import {
    Button, DropdownMenu, DropdownMenuItem, Modal, ModalBody, ModalFooter, ModalTitle, TextField,
} from '@symphony-ui/uitoolkit-components/components';
import api from '../core/api';
import { editor } from 'monaco-editor/esm/vs/editor/editor.api';
import styled from 'styled-components';
import { useRef, useState, useEffect } from 'react';

const SaveButtonRoot = styled.div`
    display: flex;
`;

const LeftButton = styled(Button)`
    border-radius: 1.75rem 0 0 1.75rem;
`;

const RightButton = styled(Button)`
    border-radius: 0 1.75rem 1.75rem 0;
    font-family: tk-icons !important;
    padding: .5rem;
    :before {
        content: ' ';
        border-right: var(--tk-color-electricity-20) 1px solid;
        position: relative;
        left: -.5rem;
    }
`;

const FloatingMenu = styled(DropdownMenu)`
    position: absolute;
    left: ${props => props.x}px;
    top: ${props => props.y}px;
    width: ${props => props.w}px;
`;

const ConfirmDiscardModal = ({ show, setShow }) => {
    const contents = useRecoilState(atoms.contents)[0];

    const discardWorkflow = () => {
        editor.getModels()[0].setValue(contents);
        setShow(false);
    };

    return (
        <Modal size="medium" show={show}>
            <ModalTitle>Discard your changes</ModalTitle>
            <ModalBody>All changes will be lost. Are you sure?</ModalBody>
            <ModalFooter>
                <Button
                    variant="primary-destructive"
                    onClick={discardWorkflow}
                >
                    Discard
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setShow(false)}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
};

const SaveWithCommentModal = ({ show, setShow, saveWorkflow, loading }) => {
    const [ comment, setComment ] = useState('');
    const commentRef = useRef();

    const submitSaveWorkflow = () => {
        saveWorkflow(comment);
        setComment('');
    };

    useEffect(() => {
        if (show && commentRef.current) {
            commentRef.current.focus();
        }
    }, [ show ]);

    return (
        <Modal size="medium" show={show}>
            <ModalTitle>Save Workflow</ModalTitle>
            <ModalBody>
                <TextField
                    ref={commentRef}
                    label="Save Comment"
                    showRequired={true}
                    value={comment}
                    disabled={loading}
                    onChange={({ target }) => setComment(target.value)}
                />
            </ModalBody>
            <ModalFooter>
                <Button onClick={submitSaveWorkflow} loading={loading}>
                    Save
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setShow(false)}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
};

const SaveButton = ({ thisEditor }) => {
    const markers = useRecoilState(atoms.markers)[0];
    const isContentChanged = useRecoilState(atoms.isContentChanged)[0];
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const { addWorkflow, listWorkflows, showStatus } = api();
    const session = useRecoilState(atoms.session)[0];
    const buttonRef = useRef();
    const setPosition = useRecoilState(atoms.position)[1];
    const [ showMenu, setShowMenu ] = useState(false);
    const [ showDiscardModal, setShowDiscardModal ] = useState(false);
    const [ showSaveModal, setShowSaveModal ] = useState(false);
    const setWorkflows = useRecoilState(atoms.workflows)[1];
    const [ currentWorkflow, setCurrentWorkflow ] = useRecoilState(atoms.currentWorkflow);
    const setActiveVersion = useRecoilState(atoms.activeVersion)[1];

    document.onkeydown = (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 's') {
            e.preventDefault();
            if (!isDisabled() && !loading) {
                saveWorkflow('Quick Save');
            }
        }
    };

    const saveWorkflow = (description) => {
        setPosition(thisEditor.getPosition());
        const swadl = editor.getModels()[0].getValue();
        setShowMenu(false);
        setLoading(true);
        addWorkflow({ swadl, createdBy: session.id, description }).then(
            () => listWorkflows((response) => {
                setLoading(false);
                showStatus(false, 'Workflow saved');
                setShowSaveModal(false);

                const values = response
                    .map(({ id, version }) => ({ label: id, value: id, version }))
                    .sort((a, b) => a.label > b.label ? 1 : -1);
                setWorkflows(values);
                const newWorkflow = values.find(w => w.value === currentWorkflow.value);
                setCurrentWorkflow(newWorkflow);
                setActiveVersion(newWorkflow.version);
            }),
            () => setLoading(false)
        );
    };

    const isDisabled = () => markers.length > 0 || isContentChanged === 'original';

    const getBottomAnchor = () => {
        const rect = buttonRef.current?.parentNode.getBoundingClientRect();
        return {
            w: rect?.width * 1.65,
            x: rect?.left,
            y: !rect ? 0 : (rect.top + rect.height),
        };
    };

    const SaveMenu = () => buttonRef.current && (
        <FloatingMenu show={showMenu} { ...getBottomAnchor() } onClick={() => setShowMenu(false)}>
            <DropdownMenuItem onClick={() => setShowSaveModal(true)}>
                Save with comment
            </DropdownMenuItem>
            <DropdownMenuItem
                onClick={() => setShowDiscardModal({ show: true })}
            >
                Discard changes
            </DropdownMenuItem>
        </FloatingMenu>
    );

    return (
        <SaveButtonRoot>
            <LeftButton
                loading={!isDisabled() && loading}
                disabled={isDisabled()}
                onClick={() => saveWorkflow('')}
            >
                Save
            </LeftButton>
            <div ref={buttonRef}>
                <RightButton
                    disabled={isDisabled()}
                    onClick={() => setShowMenu((show) => !show)}
                >
                    
                </RightButton>
            </div>
            <SaveMenu />
            <ConfirmDiscardModal
                show={showDiscardModal}
                setShow={setShowDiscardModal}
            />
            <SaveWithCommentModal
                show={showSaveModal}
                setShow={setShowSaveModal}
                saveWorkflow={saveWorkflow}
                loading={loading}
            />
        </SaveButtonRoot>
    );
};
export default SaveButton;
