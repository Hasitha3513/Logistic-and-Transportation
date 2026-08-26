import type { ReactNode } from 'react';
import { ArrowRightOutlined, CheckCircleFilled } from '@ant-design/icons';
import { Button, Card, Flex, Space, Tag, Typography } from 'antd';

const { Paragraph, Text, Title } = Typography;

interface ModulePageProps {
  eyebrow: string;
  title: string;
  description: string;
  icon: ReactNode;
}

export default function ModulePage({ eyebrow, title, description, icon }: ModulePageProps) {
  return (
    <Card className="module-card" variant="borderless">
      <Flex className="module-card__content" vertical gap={20}>
        <Flex align="center" justify="space-between" wrap gap={16}>
          <div className="module-card__icon">{icon}</div>
          <Tag icon={<CheckCircleFilled />} color="success">Connected to API</Tag>
        </Flex>
        <div>
          <Text className="module-card__eyebrow">{eyebrow}</Text>
          <Title level={3}>{title}</Title>
          <Paragraph type="secondary">{description}</Paragraph>
        </div>
        <Space wrap>
          <Button type="primary" icon={<ArrowRightOutlined />}>Open workspace</Button>
          <Button>View activity</Button>
        </Space>
      </Flex>
    </Card>
  );
}
