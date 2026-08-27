import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Flex, Form, Input, Typography } from 'antd';
import { Navigate, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useAuth } from './AuthContext';

const schema = z.object({
  username: z.string().trim().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginValues = z.infer<typeof schema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const { user, login } = useAuth();
  const { control, handleSubmit, formState: { errors, isSubmitting }, setError } = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '' },
  });

  if (user) return <Navigate to="/" replace />;

  const submit = handleSubmit(async (values) => {
    try {
      await login(values.username, values.password);
      navigate('/', { replace: true });
    } catch {
      setError('root', { message: 'Sign-in failed. Check your username and password.' });
    }
  });

  return (
    <Flex className="login-page" align="center" justify="center">
      <Card className="login-card">
        <Flex vertical gap={6} className="login-card__heading">
          <Typography.Title level={2}>TransportOps</Typography.Title>
          <Typography.Text type="secondary">Sign in to the operations control center</Typography.Text>
        </Flex>
        {errors.root && <Alert type="error" showIcon message={errors.root.message} />}
        <Form layout="vertical" onFinish={() => void submit()} requiredMark={false}>
          <Form.Item label="Username" validateStatus={errors.username ? 'error' : undefined} help={errors.username?.message}>
            <Controller name="username" control={control} render={({ field }) => <Input {...field} autoComplete="username" prefix={<UserOutlined />} />} />
          </Form.Item>
          <Form.Item label="Password" validateStatus={errors.password ? 'error' : undefined} help={errors.password?.message}>
            <Controller name="password" control={control} render={({ field }) => <Input.Password {...field} autoComplete="current-password" prefix={<LockOutlined />} />} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={isSubmitting}>Sign in</Button>
        </Form>
      </Card>
    </Flex>
  );
}
