import { useRecoilState } from 'recoil';
import { atoms } from './atoms';
import { EventSourcePolyfill } from 'event-source-polyfill';

const parseJwt = (token) => {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = window.atob(base64).split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('');
    const jwt = JSON.parse(decodeURIComponent(payload));
    const { id, displayName } = jwt.user;
    return { id, displayName, exp: jwt.exp };
};

const api = () => {
    const dev = window.location.hostname === 'localhost';
    const apiRoot = dev ? 'https://localhost:10443' : '';
    const setStatus = useRecoilState(atoms.status)[1];
    const setLoading = useRecoilState(atoms.loading)[1];
    const session = useRecoilState(atoms.session)[0];
    const showStatus = (error, content) => {
        setStatus({ show: true, error, content });
        const duration = error ? 10 : 3;
        setTimeout(() => setStatus({ show: false }), duration * 1000);
    };

    const process = async (response) => {
        if (response.ok) {
            if (response.status === 204 || response.headers.get('Content-Length') === '0') {
                return new Promise((r) => r(""));
            }
            const contentType = response.headers.get('Content-type').split(';')[0];
            return contentType === 'text/plain' ? response.text() :
                contentType === 'application/json' ? response.json() : response.blob();
        } else {
            const error = await response.json();
            throw new Error(error.detail || error.message);
        }
    };

    const handleError = ({ message }) => {
        setLoading(false);
        const msg = message.startsWith('NetworkError') || message.startsWith('Failed to fetch')
            ? 'Unable to establish connectivity' : message;
        showStatus(true, msg);
    };

    const apiCall = (method, uri, body, callback, token) => {
        const headers = { 'X-Management-Token': '', 'X-Monitoring-Token': '' };
        if (token || session?.token) {
            headers['Authorization'] = `Bearer ${token || session.token}`;
        }
        const config = { method, headers };
        if (body) {
            if (uri === 'v1/workflows' || uri === 'v1/import') {
                const formData  = new FormData();
                Object.keys(body).forEach((key) => formData.append(key, body[key]));
                config.body = formData;
            } else {
                config.body = JSON.stringify(body);
                headers['Content-Type'] = 'application/json';
            }
        }
        const promise = fetch(`${apiRoot}/${uri}`, config).then(process);
        if (!callback) {
            return promise;
        } else {
            promise.then(callback).catch(handleError);
        }
    };

    const initLogs = (callback) => {
        const headers = { 'X-Management-Token': '' };
        if (session?.token) {
            headers.Authorization = `Bearer ${session.token}`;
        }
        const es = new EventSourcePolyfill(`${apiRoot}/v1/workflows/logs`, { headers });
        es.onmessage = callback;
    };

    const [ GET, POST, PUT, DELETE ] = [ 'GET', 'POST', 'PUT', 'DELETE' ];

    return {
        getProfile: (token, callback) => apiCall(GET, 'symphony/profile', null, callback, token),
        listWorkflows: (callback) => apiCall(GET, 'v1/workflows/', null, callback),
        addWorkflow: (workflow) => apiCall(POST, 'v1/workflows', workflow),
        editWorkflow: (workflow, callback) => apiCall(PUT, 'v1/workflows', workflow, callback),
        readWorkflow: (workflowId, callback) => apiCall(GET, `v1/workflows/${workflowId}`, null, callback),
        readWorkflowVersions: (workflowId, callback) => apiCall(GET, `v1/workflows/${workflowId}?all_versions=true`, null, callback),
        deleteWorkflow: (workflowId, callback) => apiCall(DELETE, `v1/workflows/${workflowId}`, null, callback),
        deleteWorkflowVersion: (workflowId, version, callback) => apiCall(DELETE, `v1/workflows/${workflowId}?version=${version}`, null, callback),
        rollbackWorkflow: (workflowId, version, callback) => apiCall(PUT, `v1/workflows/${workflowId}?version=${version}`, null, callback),
        listGalleryCategories: (callback) => apiCall(GET, 'gallery/categories', null, callback),
        listGalleryWorkflows: (category, callback) => apiCall(GET, `gallery/${category}/workflows`, null, callback),
        readGalleryWorkflow: (category, workflow, callback) => apiCall(GET, `gallery/${category}/workflows/${workflow}`, null, callback),
        getReadme: (path, callback) => apiCall(GET, `gallery/readme/${path}`, null, callback),
        getWorkflowDefinition: (workflowId, callback) => apiCall(GET, `v1/workflows/${workflowId}/nodes`, null, callback),
        listWorkflowInstances: (workflowId, callback) => apiCall(GET, `v1/workflows/${workflowId}/instances`, null, callback),
        listWorkflowInstanceActivities: (workflowId, instanceId, callback) => apiCall(GET, `v1/workflows/${workflowId}/instances/${instanceId}/states`, null, callback),
        listWorkflowInstanceVariables: (workflowId, instanceId, callback) => apiCall(GET, `v1/workflows/${workflowId}/instances/${instanceId}/variables`, null, callback),
        getUser: (userId, callback) => apiCall(GET, `symphony/user/${userId}`, null, callback),
        getUsers: (userIds, callback) => apiCall(POST, `symphony/users`, userIds, callback),
        searchUser: (query) => apiCall(GET, `symphony/user?q=${query}`),
        exportWorkflows: () => apiCall(GET, 'v1/export'),
        importWorkflows: (data, callback) => apiCall(POST, 'v1/import', data, callback),
        listSecrets: (callback) => apiCall(GET, 'v1/workflows/secrets', null, callback),
        addSecret: (secret, callback) => apiCall(POST, 'v1/workflows/secrets', secret, callback),
        deleteSecret: (key, callback) => apiCall(DELETE, `v1/workflows/secrets/${key}`, null, callback),
        parseJwt,
        showStatus,
        initLogs,
    };
};
export default api;
