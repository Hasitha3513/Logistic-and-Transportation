import React from 'react';
import { Card, Col, DatePicker, Form, Row, Select } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { DeliveryAnalyticsFilters } from '../types/deliveryAnalytics';

const { RangePicker } = DatePicker;

interface DeliveryAnalyticsFilterBarProps {
  filters: DeliveryAnalyticsFilters;
  onFilterChange: (filters: DeliveryAnalyticsFilters) => void;
}

export const DeliveryAnalyticsFilterBar: React.FC<DeliveryAnalyticsFilterBarProps> = ({
  filters,
  onFilterChange,
}) => {
  const [form] = Form.useForm();

  const handleDateChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (dates && dates[0] && dates[1]) {
      onFilterChange({
        ...filters,
        from: dates[0].format('YYYY-MM-DD'),
        to: dates[1].format('YYYY-MM-DD'),
      });
    } else {
      const { from, to, ...rest } = filters;
      onFilterChange(rest);
    }
  };

  const handleServiceTypeChange = (serviceType?: string) => {
    onFilterChange({
      ...filters,
      serviceType: serviceType || undefined,
    });
  };

  const handlePriorityChange = (priority?: string) => {
    onFilterChange({
      ...filters,
      priority: priority || undefined,
    });
  };

  const defaultDates: [Dayjs, Dayjs] = [
    filters.from ? dayjs(filters.from) : dayjs().subtract(30, 'days'),
    filters.to ? dayjs(filters.to) : dayjs(),
  ];

  return (
    <Card style={{ marginBottom: 16 }}>
      <Form layout="vertical" form={form} initialValues={{ dateRange: defaultDates, serviceType: filters.serviceType, priority: filters.priority }}>
        <Row gutter={[16, 16]} align="bottom">
          <Col xs={24} sm={12} md={8} lg={8}>
            <Form.Item label="Period (Max 365 Days)" name="dateRange" style={{ marginBottom: 0 }}>
              <RangePicker
                style={{ width: '100%' }}
                format="YYYY-MM-DD"
                allowClear={false}
                onChange={handleDateChange}
              />
            </Form.Item>
          </Col>

          <Col xs={24} sm={12} md={8} lg={8}>
            <Form.Item label="Service Type" name="serviceType" style={{ marginBottom: 0 }}>
              <Select
                placeholder="All Service Types"
                allowClear
                onChange={handleServiceTypeChange}
                options={[
                  { value: 'STANDARD', label: 'Standard' },
                  { value: 'EXPRESS', label: 'Express' },
                  { value: 'SAME_DAY', label: 'Same Day' },
                  { value: 'SCHEDULED', label: 'Scheduled' },
                ]}
              />
            </Form.Item>
          </Col>

          <Col xs={24} sm={12} md={8} lg={8}>
            <Form.Item label="Priority" name="priority" style={{ marginBottom: 0 }}>
              <Select
                placeholder="All Priorities"
                allowClear
                onChange={handlePriorityChange}
                options={[
                  { value: 'LOW', label: 'Low' },
                  { value: 'NORMAL', label: 'Normal' },
                  { value: 'HIGH', label: 'High' },
                  { value: 'URGENT', label: 'Urgent' },
                ]}
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Card>
  );
};
