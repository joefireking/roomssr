export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface PageQuery {
  current?: number
  size?: number
}

export interface Apartment {
  id?: number
  name: string
  address?: string
  city?: string
  district?: string
  contactName?: string
  contactPhone?: string
  totalBuildings?: number
  description?: string
  status?: number
  createTime?: string
}

export interface Building {
  id?: number
  apartmentId: number
  name: string
  floors?: number
  description?: string
  status?: number
  apartmentName?: string
  createTime?: string
}

export interface RoomType {
  id?: number
  typeName: string
  area?: number
  orientation?: string
  basePrice?: number
  facilities?: string
  description?: string
}

export interface Room {
  id?: number
  buildingId: number
  roomTypeId?: number
  roomNumber: string
  floor?: number
  rentPrice?: number
  image?: string
  status?: number
  buildingName?: string
  typeName?: string
  apartmentName?: string
  createTime?: string
}

export interface Tenant {
  id?: number
  name: string
  gender?: number
  phone: string
  idCard: string
  idCardFront?: string
  idCardBack?: string
  emergencyContact?: string
  emergencyPhone?: string
  tag?: string
  remark?: string
  createTime?: string
}

export interface Contract {
  id?: number
  contractNo: string
  tenantId: number
  roomId: number
  startDate: string
  endDate: string
  rentAmount: number
  depositAmount: number
  paymentCycle?: number
  status?: number
  terminateReason?: string
  remark?: string
  tenantName?: string
  roomNumber?: string
  createTime?: string
}

export interface Bill {
  id?: number
  billNo: string
  contractId: number
  tenantId: number
  roomId: number
  billType?: number
  amount: number
  billingMonth?: string
  dueDate?: string
  status?: number
  paidTime?: string
  remark?: string
  tenantName?: string
  roomNumber?: string
  createTime?: string
}

export interface Payment {
  id?: number
  paymentNo: string
  billId: number
  tenantId: number
  amount: number
  paymentMethod?: number
  paymentTime?: string
  remark?: string
  operatorId?: number
  createTime?: string
}

export interface RepairOrder {
  id?: number
  orderNo: string
  roomId: number
  tenantId?: number
  type?: number
  priority?: number
  description?: string
  images?: string
  status?: number
  assigneeId?: number
  resolveTime?: string
  resolveRemark?: string
  roomNumber?: string
  tenantName?: string
  assigneeName?: string
  createTime?: string
}

export interface SysUser {
  id?: number
  username: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  status?: number
  createTime?: string
}

export interface SysRole {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  status?: number
}

export interface SysPermission {
  id?: number
  parentId?: number
  permissionName: string
  permissionCode: string
  type?: number
  path?: string
  icon?: string
  sortOrder?: number
  children?: SysPermission[]
}

export interface SysDict {
  id?: number
  dictType: string
  dictCode: string
  dictLabel: string
  sortOrder?: number
  status?: number
}

export interface OperationLog {
  id?: number
  userId?: number
  username?: string
  module?: string
  operation?: string
  method?: string
  params?: string
  ip?: string
  duration?: number
  status?: number
  createTime?: string
}

export interface Announcement {
  id?: number
  title: string
  content?: string
  summary?: string
  publisherId?: number
  status?: number
  topFlag?: number
  createTime?: string
  updateTime?: string
}

export interface LoginForm {
  username: string
  password: string
}
