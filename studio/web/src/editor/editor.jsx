import { atoms } from '../core/atoms';
import { setDiagnosticsOptions } from 'monaco-yaml';
import { editor, Uri } from 'monaco-editor';
import { useRecoilState } from 'recoil';
import React, { lazy, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import YamlWorker from './yaml-worker?worker';
import api from '../core/api';

const CreateWorkflowButton = lazy(() => import('../create-workflow/create-workflow-button'));

window.MonacoEnvironment = {
    getWorker: (moduleId, label) => new YamlWorker(),
};
const uri = 'https://raw.githubusercontent.com/finos/symphony-wdk/master/workflow-language/src/main/resources/swadl-schema-1.0.json';
const modelUri = Uri.parse(uri);
setDiagnosticsOptions({
    validate: true,
    enableSchemaRequest: true,
    format: true,
    hover: true,
    completion: true,
    schemas: [{ uri: uri, fileMatch: [String(modelUri)] }],
});

const Root = styled.div`
    border: #8f959e 1px solid;
    flex: 1 1 1px;
    display: ${props => props.show ? 'flex' : 'none'};
    flex-direction: column;
    justify-content: space-between;
    max-height: calc(100vh - 7rem);
`;

const EditorRoot = styled.div`
    height: ${props => (props.large ? '100%' : '80%')};
`;

const ProblemsRoot = styled.div`
    background-color: var(--tk-color-red-${props => props.theme === 'light' ? 20 : 60});
    border-top: 1px #8f959e solid;
    overflow-x: auto;
    height: 100px;
    justify-self: flex-end;
`;

const ProblemEntry = styled.div`
    font-size: .9rem;
    padding: .2rem;
    :hover {
        background-color: var(--tk-color-red-${props => props.theme === 'light' ? 30 : 40});
        cursor: pointer;
    }
`;

const EmptyRoot = styled.div`
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
`;

const Editor = ({ show, thisEditor, setThisEditor }) => {
    const ref = useRef(null);
    const { readWorkflow } = api();
    const theme = useRecoilState(atoms.theme)[0];
    const [ position, setPosition ] = useRecoilState(atoms.position);
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const activeVersion = useRecoilState(atoms.activeVersion)[0];
    const [ model, setModel ] = useState();
    const [ markers, setMarkers ] = useRecoilState(atoms.markers);
    const setIsContentChanged = useRecoilState(atoms.isContentChanged)[1];
    const [ snippet, setSnippet ] = useRecoilState(atoms.snippet);
    const [ contents, setContents ] = useRecoilState(atoms.contents);
    const [ author, setAuthor ] = useRecoilState(atoms.author);
    const session = useRecoilState(atoms.session)[0];

    useEffect(() => {
        if (!currentWorkflow) {
            setContents(undefined);
            setAuthor(undefined);
            return;
        }
        readWorkflow(currentWorkflow?.value, (response) => {
            const current = response[0];
            setContents(current.swadl);
            setAuthor(current.createdBy);
            if (position) {
                setTimeout(() => {
                    thisEditor.setPosition(position);
                    thisEditor.focus();
                    setPosition(undefined);
                }, 100);
            }
        });
    }, [ currentWorkflow, activeVersion ]);

    useEffect(() => {
        if (snippet && thisEditor) {
            const identifier = { major: 1, minor: 1 };
            const range = thisEditor.getSelection();
            let op = { identifier, range, text: snippet, forceMoveMarkers: true };
            thisEditor.executeEdits("wizard", [ op ]);
            thisEditor.focus();
            setSnippet(undefined);
        }
    }, [ snippet ]);

    useEffect(() => {
        if (!contents) {
            setThisEditor(undefined);
            setModel(undefined);
            return;
        }
        if (thisEditor) {
            thisEditor.setValue(contents);
            thisEditor.updateOptions({ readOnly: author !== session.id });
        } else {
            let yamlModel;
            if (!model) {
                if (editor.getModels().length > 0) {
                    editor.getModels().forEach((e) => e.dispose());
                }
                yamlModel = editor.createModel(contents, 'yaml', modelUri);
                setModel(yamlModel);
            } else {
                yamlModel = model;
            }
            const newEditor = editor.create(ref.current, {
                language: 'yaml',
                automaticLayout: true,
                model: yamlModel,
                theme: 'vs-' + theme,
                scrollbar: { vertical: 'hidden' },
                readOnly: author !== session.id,
            });

            newEditor.onDidChangeModelContent((e) => {
                const modifiedContents = yamlModel.getValue();
                if (modifiedContents !== contents && !e.isFlush) {
                    setIsContentChanged('modified');
                } else {
                    setIsContentChanged('original');
                }
            });
            editor.onDidChangeMarkers(({ resource }) => setMarkers(editor.getModelMarkers({ resource })));
            setThisEditor(newEditor);
        }
    }, [ contents, author ]);

    const goto = (lineNumber, column) => {
        thisEditor.revealLineInCenter(lineNumber);
        thisEditor.setPosition({ lineNumber, column });
        thisEditor.focus();
    }

    const Problems = ({ theme, markers }) => markers.map((
        { startLineNumber, startColumn, message }, index
    ) => (
        <ProblemEntry key={index} theme={theme} onClick={() => goto(startLineNumber, startColumn)}>
            {startLineNumber}: {message}
        </ProblemEntry>
    ));

    const Empty = () => (
        <EmptyRoot>
            <CreateWorkflowButton />
        </EmptyRoot>
    );

    return (
        <Root show={show}>
            { !contents ? <Empty /> : <EditorRoot ref={ref} large={markers.length === 0} /> }
            { markers.length > 0 && (
                <ProblemsRoot {...{ theme }}>
                    <Problems {...{ theme, markers }} />
                </ProblemsRoot>
            )}
        </Root>
    );
};

export default Editor;
