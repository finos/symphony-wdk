
import { useState, useEffect, useRef } from 'react';
import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle,
} from '@symphony-ui/uitoolkit-components';
import api from '../core/api';
import { atoms } from '../core/atoms';
import { useRecoilState } from 'recoil';
import { editor } from 'monaco-editor';
import Spinner from '../core/spinner';
import {
    Root, VersionsPane, Version, EditorPane, Editor, ContainedBadge, Labels,
} from  './styles';

const VersionsExplorer = ({
    versions, setVersions, currentWorkflow, getLabel,
    selectedVersion, setSelectedVersion, activeVersion, editorMode,
}) => {
    const theme = useRecoilState(atoms.theme)[0];
    const [ authors, setAuthors ] = useState();
    const [ showDeleteModal, setShowDeleteModal ] = useState(false);
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const { readWorkflowVersions, deleteWorkflowVersion, showStatus, getUsers } = api();
    const [ thisEditor, setThisEditor ] = useState();
    const contents = useRecoilState(atoms.contents)[0];
    const ref = useRef(null);

    const cleanup = () => {
        if (editor.getModels().length > 1) {
            editor.getModels().forEach((e, i) => {
                if (i > 0) {
                    e.dispose();
                }
            });
            const preserve = editorMode == 'Single' ? 0 : 1;
            while (ref.current?.childNodes.length > preserve) {
                ref.current.removeChild(ref.current.childNodes[0]);
            }
        }
    };

    useEffect(() => readWorkflowVersions(currentWorkflow.value, (response) => {
        const userIds = [ ...new Set(response.map((w) => w.createdBy).filter((u) => u)) ];
        getUsers(userIds, (users) => setAuthors(users));

        const sorted = response.map((v, i) => ({ ...v, i: i+1 }))
            .sort((a, b) => b.version - a.version);
        setVersions([
            ...sorted.filter((v) => v.active),
            ...sorted.filter((v) => !v.active),
        ]);
        setSelectedVersion(activeVersion);
    }), []);

    useEffect(() => {
        if (versions.length === 0) {
            return;
        }
        cleanup();
        const opts = {
            language: 'yaml',
            theme: 'vs-' + theme,
            readOnly: true,
            scrollBeyondLastLine: false,
            automaticLayout: true,
        };

        if (editorMode === 'Single') {
            setThisEditor(editor.create(ref.current, opts));
        } else {
            if (ref.current?.childNodes.length > 0) {
                ref.current.removeChild(ref.current.childNodes[0]);
            }
            const o = (editorMode === 'Unified') ? { ...opts, renderSideBySide: false, } : opts;
            setThisEditor(editor.createDiffEditor(ref.current, o));
        }
    }, [ editorMode, versions ])

    useEffect(() => {
        if (!thisEditor || versions.length === 0 || !selectedVersion) {
            return;
        }
        const { swadl } = versions.filter(({ version }) => version === selectedVersion)[0];
        cleanup();
        if (editorMode === 'Single') {
            thisEditor.setModel(editor.createModel(swadl, 'yaml'));
        } else {
            thisEditor.setModel({
                original: editor.createModel(contents, 'yaml'),
                modified: editor.createModel(swadl, 'yaml')
            });
        }
    }, [ thisEditor, selectedVersion ]);

    const getVariant = (active, version) =>
        active ? 'positive' : selectedVersion === version ? 'neutral' : 'default';

    const deleteVersion = () => {
        setLoading(true);
        deleteWorkflowVersion(currentWorkflow.value, selectedVersion, () => {
            setLoading(false);
            setShowDeleteModal(false);
            const newVersions = versions.filter(v => v.version !== selectedVersion);
            setSelectedVersion(newVersions[0].version);
            setVersions(newVersions);
            showStatus(false, "Workflow version deleted");
        });
    };

    const ConfirmDeleteModdal = () => (
        <Modal size="medium" show>
            <ModalTitle>Confirm Delete</ModalTitle>
            <ModalBody>This will delete this version of the workflow permanently. Are you sure?</ModalBody>
            <ModalFooter>
                <Button
                    variant="primary-destructive"
                    onClick={deleteVersion}
                    disabled={loading}
                    loading={loading}
                >
                    Delete
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setShowDeleteModal(false)}
                    disabled={loading}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );

    const VersionsList = () => versions.map(({ active, version, description, createdBy, i }) => {
        const variant = getVariant(active, version);
        const isSelected = version === selectedVersion;
        return (
            <Version
                key={version}
                selected={isSelected}
                onClick={() => setSelectedVersion(version)}
            >
                <ContainedBadge variant={variant}>
                    v{i}
                </ContainedBadge>
                <ContainedBadge variant={variant}>
                    {(new Date(version / 1000)).toLocaleString()}
                </ContainedBadge>
                <ContainedBadge variant={variant} title={!authors ? 'Loading..' : authors[createdBy]}>
                    {!authors ? 'Loading..' : authors[createdBy]}
                </ContainedBadge>
                { description === '' ? 'No comment' : description }
                { !active && isSelected && (
                    <Button
                        size="small"
                        variant="destructive"
                        onClick={() => setShowDeleteModal(true)}
                    >
                        Delete
                    </Button>
                )}
            </Version>
        );
    });

    return versions.length === 0 ? <Root><Spinner /></Root> : (
        <Root>
            <VersionsPane>
                <VersionsList />
                { showDeleteModal && <ConfirmDeleteModdal /> }
            </VersionsPane>
            <EditorPane>
                <Labels>
                    <ContainedBadge variant="positive">
                        Active Version: v{getLabel(activeVersion)}
                    </ContainedBadge>
                    <ContainedBadge variant="neutral">
                        Selected Version: v{getLabel(selectedVersion)}
                    </ContainedBadge>
                </Labels>
                <Editor ref={ref} />
            </EditorPane>
        </Root>
    );
};
export default VersionsExplorer;
