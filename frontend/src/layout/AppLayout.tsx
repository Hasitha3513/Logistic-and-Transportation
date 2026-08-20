import { useMemo, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  DownOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Alert,
  Badge,
  Breadcrumb,
  Button,
  Card,
  Descriptions,
  Dropdown,
  Drawer,
  Flex,
  Layout,
  Menu,
  Space,
  Spin,
  Tooltip,
  Tag,
  Typography,
  type MenuProps,
} from 'antd';
import { useAuth } from '../auth/AuthContext';
import { findNavigationItem, navigation, permittedNavigation, type NavigationItem } from '../navigation/navigation';
import { NotificationCenter } from '../notifications/NotificationCenter';

const { Header, Sider, Content } = Layout;
const { Text, Title } = Typography;

function toMenuItems(items: NavigationItem[]): MenuProps['items'] {
  return items.map((item) => ({
    key: item.key,
    icon: item.icon,
    label: item.path ? <Link to={item.path}>{item.label}</Link> : item.label,
    children: item.children ? toMenuItems(item.children) : undefined,
  }));
}

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [accessOpen, setAccessOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isLoading, isError, logout, isLoggingOut } = useAuth();
  const permittedItems = useMemo(
    () => permittedNavigation(navigation, user?.permissions ?? []),
    [user?.permissions],
  );
  const current = findNavigationItem(location.pathname);
  const selectedKey = current?.key ?? 'dashboard';
  const parent = navigation.find((item) => item.children?.some((child) => child.key === selectedKey));
  const pageTitle = current?.label ?? 'Workspace';
  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}` || user?.username?.[0] || 'U';

  if (isLoading) {
    return <Flex className="session-state" align="center" justify="center"><Spin size="large" aria-label="Loading workspace" /></Flex>;
  }

  if (isError || !user) {
    return (
      <Flex className="session-state" vertical align="center" justify="center" gap={16}>
        <SafetyCertificateOutlined className="session-state__icon" />
        <Title level={3}>Your session could not be loaded</Title>
        <Text type="secondary">Sign in again to access the operations workspace.</Text>
        <Button type="primary" onClick={() => navigate('/login')}>Return to sign in</Button>
      </Flex>
    );
  }

  const userMenu: MenuProps['items'] = [
    { key: 'identity', label: user.email, disabled: true, icon: <UserOutlined /> },
    { key: 'access', label: 'Access & permissions', icon: <SafetyCertificateOutlined /> },
    { type: 'divider' },
    { key: 'logout', label: 'Log out', icon: <LogoutOutlined />, danger: true },
  ];

  return (
    <Layout className="app-shell">
      <Sider
        className="app-sider"
        width={264}
        collapsedWidth={0}
        breakpoint="lg"
        trigger={null}
        collapsible
        collapsed={collapsed}
        onBreakpoint={(broken) => setCollapsed(broken)}
      >
        <Flex className="brand" align="center" gap={12}>
          <div className="brand__mark">TL</div>
          <div>
            <Text className="brand__name">TransportOps</Text>
            <Text className="brand__caption">Control Center</Text>
          </div>
        </Flex>
        <Menu
          mode="inline"
          theme="dark"
          selectedKeys={[selectedKey]}
          defaultOpenKeys={parent ? [parent.key] : []}
          items={toMenuItems(permittedItems)}
        />
        <div className="sider-status">
          <Badge status="success" />
          <Text>Systems operational</Text>
        </div>
      </Sider>

      <Layout className="workspace">
        <Header className="app-header">
          <Flex align="center" justify="space-between" gap={16}>
            <Flex align="center" gap={10}>
              <Tooltip title={collapsed ? 'Open navigation' : 'Collapse navigation'}>
                <Button
                  className="navigation-toggle"
                  type="text"
                  icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                  aria-label={collapsed ? 'Open navigation' : 'Collapse navigation'}
                  onClick={() => setCollapsed((value) => !value)}
                />
              </Tooltip>
              <div className="header-context">
                <Text type="secondary">Operations workspace</Text>
                <Text strong>{pageTitle}</Text>
              </div>
            </Flex>

            <Flex align="center" gap={12}>
              <NotificationCenter />
              <Dropdown
                menu={{
                  items: userMenu,
                  onClick: ({ key }) => {
                    if (key === 'logout') void logout();
                    if (key === 'access') setAccessOpen(true);
                  },
                }}
                placement="bottomRight"
                trigger={['click']}
              >
                <Button type="text" className="user-menu" loading={isLoggingOut}>
                  <Space size={10}>
                    <Avatar className="user-menu__avatar">{initials.toUpperCase()}</Avatar>
                    <span className="user-menu__identity">
                      <Text strong>{user.firstName} {user.lastName}</Text>
                      <Text type="secondary">{user.roles?.[0] ?? 'Team member'}</Text>
                    </span>
                    <DownOutlined className="user-menu__chevron" />
                  </Space>
                </Button>
              </Dropdown>
            </Flex>
          </Flex>
        </Header>

        <Content className="app-content">
          <div className="page-heading">
            <Breadcrumb
              items={[
                { title: <Link to="/">Home</Link> },
                ...(parent ? [{ title: parent.label }] : []),
                ...(location.pathname === '/' ? [] : [{ title: pageTitle }]),
              ]}
            />
            <Title level={2}>{pageTitle}</Title>
          </div>
          <main className="page-surface">
            <Outlet />
          </main>
        </Content>
      </Layout>
      <Drawer title="Access & permissions" open={accessOpen} width={560} onClose={() => setAccessOpen(false)}>
        <Flex vertical gap={20}>
          <Descriptions bordered size="small" column={1} items={[
            { key: 'account', label: 'Account', children: user.username },
            { key: 'email', label: 'Email', children: user.email },
            { key: 'status', label: 'Status', children: <Badge status={user.active ? 'success' : 'error'} text={user.active ? 'Active' : 'Disabled'} /> },
            { key: 'roles', label: 'Roles', children: <Space wrap>{user.roles.map((role) => <Tag color="blue" key={role}>{role.replaceAll('_', ' ')}</Tag>)}</Space> },
          ]} />
          <Card size="small" title="Business permissions" extra={<Badge count={user.permissions.length} overflowCount={99} />}>
            <Space size={[6, 8]} wrap>
              {[...user.permissions].sort().map((permission) => <Tag color="green" key={permission}>{permission.replaceAll('_', ' ')}</Tag>)}
            </Space>
          </Card>
          <Alert type="info" showIcon message="Backend authorization remains authoritative" description="Controls are shown from your current permissions. Every command is revalidated by Spring Security before business data is changed." />
        </Flex>
      </Drawer>
    </Layout>
  );
}
