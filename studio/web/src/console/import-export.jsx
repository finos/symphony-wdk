import { atoms } from '../core/atoms';
import { Button, Loader } from '@symphony-ui/uitoolkit-components/components';
import { useDropzone } from 'react-dropzone';
import { useEffect, useState } from 'react';
import { useRecoilState } from 'recoil';
import api from '../core/api';
import styled from 'styled-components';

const getColor = (props) => {
    if (!props.disabled) {
        if (props.isDragAccept) {
            return 'var(--tk-color-green-50)';
        }
        if (props.isDragReject) {
            return 'var(--tk-color-red-50)';
        }
        if (props.isFocused) {
            return 'var(--tk-color-electricity-50)';
        }
    }
    return 'var(--tk-button-color-secondary-default, #717681)';
};

const ImportZone = styled.div`
    width: 8rem;
    font-size: .75rem;
    text-transform: uppercase;
    color: ${props => getColor(props)};
    padding: .5rem 1rem;
    display: flex;
    justify-content: center;
    align-items: center;
    border-width: 2px;
    border-style: dashed;
    border-color: ${props => getColor(props)};
    border-radius: 1rem;
    cursor: ${props => props.disabled ? 'no-drop' : 'pointer'};
`;

const ImportExport = () => {
    const [ workflows, setWorkflows ] = useRecoilState(atoms.workflows);
    const { exportWorkflows, importWorkflows, showStatus } = api();
    const [ loading, setLoading ] = useState(false);
    const {
        getRootProps,
        getInputProps,
        isFocused,
        isDragAccept,
        isDragReject,
        acceptedFiles
    } = useDropzone({
        accept: {
            'application/zip': ['.zip'],
            'application/x-zip-compressed': ['.zip'],
        }
    });

    useEffect(() => {
        if (loading || acceptedFiles.length !== 1) {
            return;
        }
        setLoading(true);
        importWorkflows({ file: acceptedFiles[0] }, () => {
            setWorkflows(undefined);
            setLoading(false);
            showStatus(false, 'Import successful');
        });
    }, [ acceptedFiles ]);

    const performExport = () => {
        setLoading(true);
        exportWorkflows().then((blob) => {
            const href = URL.createObjectURL(blob);
            const a = Object.assign(document.createElement("a"), {
                href,
                style: 'display:none',
                download: `wdk-export-${new Date().getTime()}.zip`,
            });
            document.body.appendChild(a);
            a.click();
            URL.revokeObjectURL(href);
            a.remove();
            setLoading(false);
        });
    };

    return (
        <>
            <Button
                variant="secondary"
                loading={loading}
                onClick={performExport}
                disabled={workflows?.length === 0}
            >
                Export Workflows
            </Button>
            <ImportZone {...getRootProps({isFocused, isDragAccept, isDragReject})} disabled={loading}>
                <input {...getInputProps()} disabled={loading} />
                { loading ? <Loader /> : 'Import Workflows' }
            </ImportZone>
        </>
    );
};
export default ImportExport;
