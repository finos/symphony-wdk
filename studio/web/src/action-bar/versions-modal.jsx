import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle,
} from '@symphony-ui/uitoolkit-components/components';
import { atoms } from '../core/atoms';
import { useRecoilState } from 'recoil';
import { useState } from 'react';
import VersionsExplorer from '../versions/versions';
import api from '../core/api';
import styled from 'styled-components';

const GroupButton = styled(Button)`
    border-radius: 0;
    box-shadow: none;
    border-width: .125rem 0;
    border-color: ${props => props.variant === 'primary' ? 'var(--tk-color-electricity-30)' : 'var(--tk-button-color-secondary-hover-text, #717681);'};
    border-style: solid;
    line-height: normal;

    &:hover { box-shadow: none }
    &:first-child { border-radius: 1.75rem 0 0 1.75rem; border-left-width: .125rem }
    &:last-child  { border-radius: 0 1.75rem 1.75rem 0; border-right-width: .125rem }
    &.primary{ border-width: .125rem }
    &:not(.primary) + *:not(.primary) { border-left-width: .125rem }
`;

const VersionsModal = ({ setShow, readOnly }) => {
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const [ versions, setVersions ] = useState([]);
    const [ activeVersion, setActiveVersion ] = useRecoilState(atoms.activeVersion);
    const [ selectedVersion, setSelectedVersion ] = useState();
    const [ editorMode, setEditorMode ] = useState('Side-by-Side');
    const { rollbackWorkflow, showStatus } = api();

    const getLabel = (version) => versions.filter(v => v.version === version)[0]?.i;

    const submitRevert = () => {
        setLoading(true);
        rollbackWorkflow(currentWorkflow.value, selectedVersion, () => {
            setShow(false);
            setLoading(false);
            setActiveVersion(selectedVersion);
            showStatus(false, `Workflow ${currentWorkflow.value} reverted to v${getLabel(selectedVersion)}`);
        });
    };

    return (
        <Modal size="full-width" show closeButton onClose={() => setShow(false)}>
            <ModalTitle>Versions Explorer</ModalTitle>
            <ModalBody style={{ display: 'flex' }}>
                <VersionsExplorer {...{
                    versions,
                    setVersions,
                    currentWorkflow,
                    selectedVersion,
                    setSelectedVersion,
                    activeVersion,
                    getLabel,
                    editorMode,
                }} />
            </ModalBody>
            <ModalFooter style={{ justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', gap: '.5rem' }}>
                    <Button
                        disabled={readOnly || activeVersion === selectedVersion}
                        onClick={submitRevert}
                        loading={loading}
                    >
                        Revert to this version
                    </Button>
                    <Button variant="secondary" onClick={() => setShow(false)}>
                        Cancel
                    </Button>
                </div>
                <div>
                    {[ 'Single', 'Side-by-Side', 'Unified' ].map((mode) => (
                        <GroupButton
                            key={mode}
                            className={ editorMode === mode ? 'primary' : '' }
                            variant={ editorMode === mode ? 'primary' : 'secondary' }
                            onClick={() => setEditorMode(mode)}
                        >
                            {mode}
                        </GroupButton>
                    ))}
                </div>
            </ModalFooter>
        </Modal>
    );
};
export default VersionsModal;
