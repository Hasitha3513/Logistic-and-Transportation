import { useState } from 'react';
import { EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, DatePicker, Flex, Input, Select, Space, Table, Typography, type TableColumnsType } from 'antd';
import type { Dayjs } from 'dayjs';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { FuelPurchaseStatusTag } from '../components/status/StatusTags';
import { useFuelPurchases, useFuelVendors } from './hooks/useFuelPurchases';
import type { FuelPurchase } from './purchaseTypes';

const statuses = ['DRAFT','SUBMITTED','APPROVED','RECEIVED','RECONCILED','CANCELLED'].map(value => ({ value, label: value.replaceAll('_',' ') }));
export default function FuelPurchaseListPage() {
  const { hasPermission } = useAuth(); const [page,setPage]=useState(1); const [limit,setLimit]=useState(10);
  const [search,setSearch]=useState<string>(); const [vendorId,setVendorId]=useState<string>(); const [fuelType,setFuelType]=useState<string>();
  const [status,setStatus]=useState<string>(); const [period,setPeriod]=useState<[Dayjs|null,Dayjs|null]|null>(null);
  const vendors=useFuelVendors(); const query=useFuelPurchases({page:page-1,limit,search,vendorId,fuelType,status,fromDate:period?.[0]?.format('YYYY-MM-DD'),toDate:period?.[1]?.format('YYYY-MM-DD')});
  if (!hasPermission('FUEL_PURCHASE_VIEW')) return <Navigate to="/workspace" replace />;
  const money=(value:number,row:FuelPurchase)=>new Intl.NumberFormat(undefined,{style:'currency',currency:row.currencyCode}).format(value);
  const columns:TableColumnsType<FuelPurchase>=[
    {title:'Purchase number',dataIndex:'purchaseNumber',render:(v,row)=><Link to={`/fuel/purchases/${row.id}`}><Typography.Text strong>{v}</Typography.Text></Link>},
    {title:'Date',dataIndex:'purchaseDate'}, {title:'Vendor',render:(_,row)=>row.vendor.name}, {title:'Invoice',dataIndex:'invoiceNumber',render:v=>v??'—'},
    {title:'Fuel type',dataIndex:'fuelType'}, {title:'Quantity',dataIndex:'quantity',align:'right'},
    {title:'Unit price',dataIndex:'unitPrice',align:'right',render:(v,row)=>money(v,row)}, {title:'Tax',dataIndex:'taxAmount',align:'right',render:(v,row)=>money(v,row)},
    {title:'Total',dataIndex:'totalAmount',align:'right',render:(v,row)=>money(v,row)}, {title:'Status',dataIndex:'status',render:v=><FuelPurchaseStatusTag status={v}/>},
    {title:'Reconciliation',dataIndex:'reconciliationStatus',responsive:['lg'],render:v=>v.replaceAll('_',' ')},
    {title:'Actions',render:(_,row)=><Link to={`/fuel/purchases/${row.id}`}><Button type="link" icon={<EyeOutlined/>}>View</Button></Link>},
  ];
  return <Flex vertical gap={18}>
    <Flex justify="space-between" align="center" wrap gap={12}><div><Typography.Title level={3}>Fuel purchases</Typography.Title><Typography.Text type="secondary">Invoice, receipt, variance and reconciliation tracking.</Typography.Text></div><Space>{hasPermission('FUEL_PURCHASE_CREATE')&&<Link to="/fuel/purchases/new"><Button type="primary" icon={<PlusOutlined/>}>New purchase</Button></Link>}<Button icon={<ReloadOutlined/>} loading={query.isFetching} onClick={()=>void query.refetch()}>Refresh</Button></Space></Flex>
    <Card variant="borderless"><Flex wrap gap={12}><Input.Search aria-label="Purchase or invoice search" placeholder="Purchase or invoice" allowClear onSearch={v=>{setSearch(v||undefined);setPage(1)}} style={{maxWidth:240}}/><Select aria-label="Vendor" placeholder="All vendors" allowClear showSearch optionFilterProp="label" options={(vendors.data??[]).map(v=>({value:v.id,label:`${v.code} — ${v.name}`}))} onChange={v=>{setVendorId(v);setPage(1)}} style={{minWidth:220}}/><Select aria-label="Fuel type" placeholder="All fuel types" allowClear options={['DIESEL','PETROL','ELECTRIC','OTHER'].map(value=>({value}))} onChange={v=>{setFuelType(v);setPage(1)}} style={{minWidth:150}}/><Select aria-label="Purchase status" placeholder="All statuses" allowClear options={statuses} onChange={v=>{setStatus(v);setPage(1)}} style={{minWidth:180}}/><DatePicker.RangePicker value={period} onChange={v=>{setPeriod(v);setPage(1)}}/></Flex></Card>
    {query.isError&&<Alert type="error" showIcon message="Fuel purchases could not be loaded"/>}
    <Card><Table rowKey="id" columns={columns} dataSource={query.data?.content??[]} loading={query.isLoading} scroll={{x:1200}} pagination={{current:page,pageSize:limit,total:query.data?.totalElements??0,showSizeChanger:true,onChange:(p,s)=>{setPage(p);setLimit(s)}}} locale={{emptyText:'No fuel purchases found'}}/></Card>
  </Flex>;
}
