import { expect, request, test } from '@playwright/test';

const backend=process.env.REAL_E2E_BACKEND_URL??'http://localhost:8088';const suffix=`cs09-${Date.now()}`;
type Auth={accessToken:string;refreshToken:string};
type Provider={id:string;displayName:string;providerAlias:string;providerType:string;lifecycle:string};
type Device={id:string;externalReference:string;lifecycle:string;version:number;currentProviderBinding?:{bindingVersion:number;providerAlias:string;maskedExternalDeviceReference:string};currentVehicleAssociation?:{vehicleId:string}};
type Page<T>={items:T[]};

test.describe.serial('US-48 CS09 device onboarding real PostgreSQL journey',()=>{
  let admin:Auth;let limited:Auth;let tenantB:Auth;let provider:Provider;let vehicle:{id:string;registrationNumber:string};let device:Device;

  test.beforeAll(async()=>{
    admin=await login();const api=await authorized(admin);
    const providers=await json<Page<Provider>>(api.get('/api/v1/tracking/provider-connections?page=0&size=100'));
    provider=providers.items.find(item=>item.lifecycle==='ACTIVE')!;expect(provider).toBeTruthy();
    const categories=entities(await json(api.get('/api/vehicle-categories')));const types=entities(await json(api.get('/api/vehicle-types')));
    const registrationNumber=`CS09-${Date.now()}`;const created=await api.post('/api/vehicles',{data:{registrationNumber,categoryId:categories[0].id,typeId:types[0].id,chassisNumber:`CS09-CH-${Date.now()}`,engineNumber:`CS09-EN-${Date.now()}`,manufacturer:'Acceptance',model:'CS09',manufactureYear:2026,ownershipType:'COMPANY_OWNED',operationalStatus:'AVAILABLE',active:true}});
    expect(created.status(),await created.text()).toBe(201);vehicle=await created.json() as typeof vehicle;
    limited=await createUser(api,['TRACKING_VIEW']);const tenant=await api.post('/api/e2e/tenant-fixtures',{data:{suffix}});expect(tenant.status(),await tenant.text()).toBe(201);const other=await tenant.json() as {username:string;password:string};tenantB=await login(other.username,other.password);await api.dispose();
  });

  test('1/3 completes DRAFT-first bind, association, reload, lifecycle, rebind, and retirement',async({page})=>{
    test.setTimeout(120_000);
    await authenticate(page,admin);await page.goto('/tracking/devices');
    await page.getByRole('button',{name:/Add Device/}).first().click();
    await chooseFirst(page,'Active provider connection');
    const externalReference=`cs09-device-${Date.now()}`;await page.getByLabel('External device reference').fill(externalReference);await page.getByLabel('Hardware serial reference').fill(`serial-${suffix}`);
    await page.getByRole('dialog').last().getByRole('button',{name:'Create DRAFT'}).click();await expect(page.getByText('Draft tracking device created')).toBeVisible();
    const api=await authorized(admin);const devices=await json<Device[]>(api.get('/api/v1/tracking/devices?page=0&size=100'));device=devices.find(item=>item.externalReference===externalReference)!;expect(device.lifecycle).toBe('DRAFT');expect(device.currentProviderBinding).toBeFalsy();
    await page.getByRole('button',{name:'Bind Provider'}).click();await chooseFirst(page,'Active provider connection');await page.getByRole('dialog').last().getByLabel('External device reference').fill(externalReference);await page.getByRole('dialog').last().getByRole('button',{name:'Bind Provider'}).click();await expect(page.getByText('Provider bound')).toBeVisible();
    await page.getByRole('button',{name:'Associate Vehicle'}).click();await chooseSearch(page,'Vehicle',vehicle.registrationNumber);await page.getByRole('dialog').last().getByRole('button',{name:'OK'}).click();await expect(page.getByText('Vehicle associated')).toBeVisible();

    await page.reload();const row=page.getByRole('row',{name:new RegExp(externalReference)});await row.getByRole('button',{name:/Details/}).click();await expect(page.getByText(new RegExp(`${provider.displayName}.*${provider.providerType}.*${provider.providerAlias}`))).toBeVisible();await expect(page.getByText(new RegExp(vehicle.registrationNumber)).last()).toBeVisible();await expect(page.getByRole('button',{name:'Activate Device'})).toBeEnabled();
    await page.getByRole('button',{name:'Activate Device'}).click();await confirm(page,'Activate device?','Activate Device');await expect(page.getByText('Device activated')).toBeVisible();
    await page.getByRole('button',{name:'Disable'}).click();await confirm(page,'Disable device?','Disable');await expect(page.getByText('Device disabled')).toBeVisible();
    await page.getByRole('button',{name:'Reactivate'}).click();await confirm(page,'Activate device?','Activate Device');await expect(page.getByText('Device activated')).toBeVisible();

    device=await json<Device>(api.get(`/api/v1/tracking/devices/${device.id}`));const oldVersion=device.currentProviderBinding!.bindingVersion;
    await page.reload();await page.getByRole('row',{name:new RegExp(externalReference)}).getByRole('button',{name:/Details/}).click();await page.getByRole('button',{name:'Rebind Provider'}).click();await page.getByRole('dialog').last().getByLabel('External device reference').fill(`${externalReference}-rebound`);const rebindRequest=page.waitForRequest(request=>request.method()==='POST'&&request.url().endsWith(`/api/v1/tracking/devices/${device.id}/provider-bindings`));await page.getByRole('dialog').last().getByRole('button',{name:'Rebind'}).click();expect((await rebindRequest).postDataJSON().currentBindingVersion).toBe(oldVersion);await expect(page.getByText('Provider rebound')).toBeVisible();device=await json<Device>(api.get(`/api/v1/tracking/devices/${device.id}`));expect(device.currentProviderBinding?.maskedExternalDeviceReference).toBeTruthy();
    await page.getByRole('button',{name:'Retire Device'}).click();await expect(page.getByText(/action is permanent.*Historical Tracking data is preserved/i)).toBeVisible();await confirm(page,'Retire device?','Retire');await expect(page.getByText('Device retired')).toBeVisible();await expect(page.getByText('Retired device')).toBeVisible();await expect(page.getByRole('button',{name:/Bind|Associate|Activate|Disable|Rebind|Retire/})).toHaveCount(0);await api.dispose();
  });

  test('2/3 read-only role has no management UI or mutation authority',async({page})=>{
    const api=await authorized(limited);expect((await api.post(`/api/v1/tracking/devices/${device.id}/disable`,{data:{version:device.version}})).status()).toBe(403);const visibleDevices=await json<Device[]>(api.get('/api/v1/tracking/devices?page=0&size=100'));const deviceIndex=visibleDevices.findIndex(item=>item.id===device.id);expect(deviceIndex).toBeGreaterThanOrEqual(0);expect(visibleDevices[deviceIndex].externalReference).toMatch(/^\*+/);await api.dispose();await authenticate(page,limited);await page.goto('/tracking/devices');await expect(page.getByRole('button',{name:/Add Device/})).toHaveCount(0);await page.locator('tbody tr.ant-table-row').nth(deviceIndex).getByRole('button',{name:/Details/}).click();await expect(page.getByRole('button',{name:/Bind|Associate|Activate|Disable|Rebind|Retire/})).toHaveCount(0);
  });

  test('3/3 Tenant B cannot read Tenant A onboarding resources',async()=>{
    const api=await authorized(tenantB);expect((await api.get(`/api/v1/tracking/devices/${device.id}`)).status()).toBe(404);expect((await api.get(`/api/v1/tracking/provider-connections/${provider.id}`)).status()).toBe(404);await api.dispose();
  });
});

async function chooseFirst(page:import('@playwright/test').Page,label:string){const input=page.getByRole('dialog').last().getByLabel(label);await input.press('ArrowDown');await input.press('Enter');}
async function chooseSearch(page:import('@playwright/test').Page,label:string,value:string){const input=page.getByRole('dialog').last().getByLabel(label);await input.fill(value);await input.press('ArrowDown');await input.press('Enter');}
async function confirm(page:import('@playwright/test').Page,title:string,button:string){await page.getByRole('dialog').filter({hasText:title}).getByRole('button',{name:button,exact:true}).click();}
async function login(username=process.env.E2E_ADMIN_USERNAME??'admin',password=process.env.E2E_ADMIN_PASSWORD??'AdminPass!2026'){const api=await request.newContext({baseURL:backend});const response=await api.post('/api/auth/login',{data:{username,password}});expect(response.status(),await response.text()).toBe(200);const auth=await response.json() as Auth;await api.dispose();return auth;}
function authorized(auth:Auth){return request.newContext({baseURL:backend,extraHTTPHeaders:{Authorization:`Bearer ${auth.accessToken}`}});}
async function json<T=unknown>(promise:Promise<import('@playwright/test').APIResponse>){const response=await promise;expect(response.status(),await response.text()).toBe(200);return response.json() as Promise<T>;}
function entities(value:unknown):Array<{id:string}>{return Array.isArray(value)?value as Array<{id:string}>:((value as {content?:Array<{id:string}>}).content??[]);}
async function createUser(api:Awaited<ReturnType<typeof request.newContext>>,permissions:string[]){const role=await api.post('/api/roles',{data:{name:`CS09 limited ${suffix}`,active:true,permissions}});expect(role.status(),await role.text()).toBe(201);const roleId=(await role.json() as {id:string}).id;const username=`cs09-limited-${Date.now()}`;const password=`Cs09Limited!${Date.now()}`;const user=await api.post('/api/users',{data:{username,email:`${username}@example.test`,password,firstName:'CS09',lastName:'Limited',active:true,roleIds:[roleId]}});expect(user.status(),await user.text()).toBe(201);return login(username,password);}
async function authenticate(page:import('@playwright/test').Page,auth:Auth){await page.addInitScript(value=>{localStorage.setItem('transport.accessToken',value.accessToken);localStorage.setItem('transport.refreshToken',value.refreshToken);},auth);}
