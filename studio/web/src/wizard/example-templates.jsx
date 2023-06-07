import styled from 'styled-components';

const templates = {
Simple: `<div>Hello <b>World!</b></div>`,
Table: `
<table>
  <thead>
    <tr>
      <td>Header 1</td>
      <td>Header 2</td>
      <td>Header 3</td>
      <td>Header 4</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td colspan="2">Content 1.1 with colspan</td>
      <td>Content 3.1</td>
      <td>Content 4.1</td>
    </tr>
    <tr>
      <td rowspan="2">Content 1.2 with rowspan</td>
      <td>Content 2.2</td>
      <td>Content 3.2</td>
      <td>Content 4.2</td>
    </tr>
    <tr>
      <td>Content 2.3</td>
      <td>Content 3.3</td>
      <td>Content 4.3</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td>Footer 1</td>
      <td>Footer 2</td>
      <td>Footer 3</td>
      <td>Footer 4</td>
    </tr>
  </tfoot>
</table>`,
Form: `
<form id="AddressForm">
  <text-field name="address" placeholder="Type your address..." required="true" />
  <select name="city">
    <option selected="true" value="ny">New York</option>
    <option value="van">Vancouver</option>
    <option value="par">Paris</option>
  </select>
  <button name="submit" type="action">Submit</button>
  <button type="reset">Reset</button>
</form>`,
Card: `
<card accent="tempo-bg-color--blue" iconSrc="./images/favicon.png">
  <header>Card Header. Always visible.</header>
  <body>Card Body. User must click to view it.</body>
</card>`,
Mention: `<mention uid="12345678" />`,
};

const ExampleTemplatesRoot = styled.div`
    font-size: .9rem;
    display: flex;
    gap: .5rem;
`;

const Example = styled.div`
    cursor: pointer;
    &:hover { text-decoration: underline }
`;

const insertTemplate = (content) => {
    const contentArea = document.querySelector('textarea[name=content]');
    contentArea.value += ('\n' + content).trim();
    contentArea.scrollTop = 999999999999;
};

const ExampleTemplates = () => {
    return (
        <ExampleTemplatesRoot>
            Examples:
            { Object.keys(templates).map((key) => (
                <Example key={key} onClick={() => insertTemplate(templates[key])}>
                    {key}
                </Example>
            ))}
        </ExampleTemplatesRoot>
    );
}
export default ExampleTemplates;
