import { DashboardOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import ModulePage from './ModulePage';

export default function WorkspacePage() {
  const navigate = useNavigate();

  return (
    <ModulePage
      eyebrow="Operations"
      title="Workspace"
      description="Select an available module from the navigation."
      icon={<DashboardOutlined />}
      onOpen={() => navigate('/')}
    />
  );
}
