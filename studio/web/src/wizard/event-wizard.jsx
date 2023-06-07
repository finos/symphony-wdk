import { useState } from 'react';
import { Dropdown, TextField } from "@symphony-ui/uitoolkit-components/components";

const EventWizard = () => {
    const events = [
        { label: 'Message Received', value: 'message-received', fieldLabel: 'Content' },
        { label: 'Form Replied', value: 'form-replied', fieldLabel: 'Form ID' },
        { label: 'Activity Completed', value: 'activity-completed', fieldLabel: 'Activity ID' },
        { label: 'Activity Expired', value: 'activity-expired', fieldLabel: 'Activity ID' }
    ];

    const [ selectedEvent, setSelectedEvent ] = useState(events[0]);

    return (
        <>
            Define the event that will trigger the activity
            <Dropdown label="Select an event" options={events} name="event" value={selectedEvent} onChange={(e) => setSelectedEvent(e.target.value)} />
            <TextField label={selectedEvent.fieldLabel} name="eventValue" />
        </>
    );
};
export default EventWizard;
