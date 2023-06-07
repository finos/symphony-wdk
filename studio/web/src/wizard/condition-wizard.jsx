import { TextField } from "@symphony-ui/uitoolkit-components/components";

const ConditionWizard = () => (
    <>
        Define the conditional execution of the activity
        <TextField name="condition" label="Execute the activity only if" />
    </>
);
export default ConditionWizard;
