// ─── Common ─────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ─── Auth ───────────────────────────────────────────────

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  tenantId: string;
  createdAt: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}

export interface PrimaryAgentInfo {
  id: string;
  name: string;
  role: string;
}

export interface MeResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  tenantId: string;
  primaryAgentInstance: PrimaryAgentInfo | null;
}

// ─── LLM Config ─────────────────────────────────────────

export interface LlmConfig {
  provider: string;
  defaultModel: string;
  hasApiKey: boolean;
}

export interface SaveLlmConfigRequest {
  provider: string;
  apiKey: string;
  defaultModel: string;
}

// ─── Agent Infrastructure ───────────────────────────────

export interface AgentTemplate {
  id: string;
  name: string;
  role: string;
  description: string;
  allowedTools: string;
  maxTokensPerRun: number;
  dailyTokenBudget: number;
  status: string;
}

export interface AgentInstance {
  id: string;
  templateId: string;
  name: string;
  parentInstanceId: string | null;
  type: string;
  status: string;
  config: string;
  customSystemPrompt: string | null;
  createdAt: string;
}

// ─── Production ─────────────────────────────────────────

export type JobStatus = 'DRAFT' | 'RELEASED' | 'IN_PRODUCTION' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type QualityResult = 'PASS' | 'FAIL' | 'PARTIAL';
export type SeverityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface JobResponse {
  id: string;
  jobNumber: string;
  customerId: string | null;
  title: string;
  status: JobStatus;
  priority: number;
  quantity: number;
  deadline: string | null;
  notes: string | null;
  createdBy: string | null;
  assignedStationId: string | null;
  shiftId: string | null;
  startedAt: string | null;
  completedAt: string | null;
  statusHistory: JobStatusHistoryResponse[];
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface JobStatusHistoryResponse {
  id: string;
  fromStatus: JobStatus | null;
  toStatus: JobStatus;
  changedBy: string | null;
  changedAt: string;
  reason: string | null;
}

export interface CreateJobRequest {
  jobNumber: string;
  customerId?: string;
  title: string;
  priority: number;
  quantity: number;
  deadline?: string;
  notes?: string;
  createdBy?: string;
}

export interface UpdateJobRequest {
  customerId?: string;
  title?: string;
  priority?: number;
  quantity?: number;
  deadline?: string;
  notes?: string;
}

export interface ChangeJobStatusRequest {
  newStatus: JobStatus;
  changedBy?: string;
  reason?: string;
}

export interface AssignJobRequest {
  stationId: string;
  shiftId: string;
}

export interface StationResponse {
  id: string;
  name: string;
  description: string | null;
  capacityPerShift: number | null;
  status: string;
  totalCapacity: number;
  shiftIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateStationRequest {
  name: string;
  description?: string;
  capacityPerShift?: number;
  status?: string;
}

export interface ShiftResponse {
  id: string;
  name: string;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
  capacityHours: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface QualityCheckResponse {
  id: string;
  jobId: string;
  checkedBy: string | null;
  checkType: string;
  result: QualityResult;
  defectCount: number;
  notes: string | null;
  checkedAt: string;
  defects: QualityDefectResponse[];
}

export interface QualityDefectResponse {
  id: string;
  defectType: string;
  description: string | null;
  severity: SeverityLevel;
}

export interface CreateQualityCheckRequest {
  jobId: string;
  checkedBy?: string;
  checkType: string;
  result: QualityResult;
  defectCount: number;
  notes?: string;
}

// ─── Machines ───────────────────────────────────────────

export type MachineStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'BLOCKED' | 'DECOMMISSIONED';
export type MaintenanceType = 'TIME_BASED' | 'HOURS_BASED';
export type MaintenanceRecordStatus = 'PLANNED' | 'IN_PROGRESS' | 'DONE' | 'SKIPPED';

export interface MachineResponse {
  id: string;
  name: string;
  machineNumber: string;
  type: string | null;
  stationId: string | null;
  status: MachineStatus;
  capacityPerHour: number | null;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  purchaseDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMachineRequest {
  name: string;
  machineNumber: string;
  type?: string;
  stationId?: string;
  capacityPerHour?: number;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  purchaseDate?: string;
}

export interface MaintenanceIntervalResponse {
  id: string;
  machineId: string;
  type: MaintenanceType;
  intervalDays: number | null;
  intervalHours: number | null;
  lastPerformedAt: string | null;
  nextDueAt: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceRecordResponse {
  id: string;
  machineId: string;
  intervalId: string | null;
  performedBy: string | null;
  performedAt: string;
  durationMinutes: number | null;
  notes: string | null;
  status: MaintenanceRecordStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MachineIncidentResponse {
  id: string;
  machineId: string;
  reportedBy: string | null;
  type: string;
  description: string | null;
  severity: SeverityLevel;
  reportedAt: string;
  resolvedAt: string | null;
  resolutionNotes: string | null;
}

export interface CreateMaintenanceIntervalRequest {
  machineId: string;
  type: MaintenanceType;
  intervalDays?: number;
  intervalHours?: number;
  nextDueAt?: string;
  description?: string;
}

export interface PerformMaintenanceRequest {
  machineId: string;
  intervalId?: string;
  performedBy: string;
  durationMinutes: number;
  notes?: string;
}

export interface ReportIncidentRequest {
  reportedBy?: string;
  type: string;
  description?: string;
  severity: SeverityLevel;
}

// ─── Inventory ──────────────────────────────────────────

export type StockMovementType = 'INBOUND' | 'OUTBOUND' | 'TRANSFER' | 'CORRECTION';

export interface ArticleResponse {
  id: string;
  articleNumber: string;
  name: string;
  description: string | null;
  categoryId: string | null;
  unitId: string | null;
  minStock: number | null;
  reorderPoint: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateArticleRequest {
  articleNumber: string;
  name: string;
  description?: string;
  categoryId?: string;
  unitId?: string;
  minStock?: number;
  reorderPoint?: number;
  status?: string;
}

export interface StockResponse {
  id: string;
  articleId: string;
  warehouseLocationId: string | null;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  updatedAt: string;
}

export interface StockSummaryResponse {
  articleId: string;
  totalQuantity: number;
  totalReserved: number;
  totalAvailable: number;
  locations: StockResponse[];
}

export interface MovementResponse {
  id: string;
  articleId: string;
  fromLocationId: string | null;
  toLocationId: string | null;
  quantity: number;
  type: StockMovementType;
  referenceType: string | null;
  referenceId: string | null;
  performedBy: string | null;
  notes: string | null;
  createdAt: string;
}

export interface CreateMovementRequest {
  articleId: string;
  fromLocationId?: string;
  toLocationId?: string;
  quantity: number;
  type: string;
  referenceType?: string;
  referenceId?: string;
  performedBy?: string;
  notes?: string;
}

export interface SupplierResponse {
  id: string;
  name: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  taxId: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  name: string;
  contactName?: string;
  email?: string;
  phone?: string;
  address?: string;
  taxId?: string;
}

export interface SupplierArticleResponse {
  id: string;
  supplierId: string;
  articleId: string;
  supplierArticleNumber: string | null;
  leadTimeDays: number | null;
  minOrderQuantity: number | null;
  createdAt: string;
}

export interface CriticalArticleResponse {
  articleId: string;
  warehouseLocationId: string | null;
  currentQuantity: number;
  minStock: number;
  deficit: number;
}

export interface CategoryResponse {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: string;
}

export interface UnitResponse {
  id: string;
  name: string;
  abbreviation: string;
}

// ─── People ─────────────────────────────────────────────

export type EmployeeStatus = 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE';
export type TimeEntryType = 'CLOCK_IN' | 'CLOCK_OUT' | 'JOB_START' | 'JOB_END';
export type AbsenceType = 'VACATION' | 'SICK' | 'OTHER';
export type AbsenceStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface EmployeeResponse {
  id: string;
  userId: string | null;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  role: string | null;
  status: EmployeeStatus;
  hireDate: string | null;
  stationId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmployeeRequest {
  userId?: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  role?: string;
  hireDate?: string;
  stationId?: string;
}

export interface TimeEntryResponse {
  id: string;
  employeeId: string;
  type: TimeEntryType;
  jobId: string | null;
  timestamp: string;
  createdAt: string;
}

export interface MyDayResponse {
  employeeId: string;
  clockedIn: boolean;
  entries: TimeEntryResponse[];
}

export interface AbsenceResponse {
  id: string;
  employeeId: string;
  type: AbsenceType;
  fromDate: string;
  toDate: string;
  status: AbsenceStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAbsenceRequest {
  employeeId: string;
  type: string;
  fromDate: string;
  toDate: string;
  notes?: string;
}

export interface QualificationResponse {
  id: string;
  employeeId: string;
  qualification: string;
  certifiedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}

// ─── Inbox ──────────────────────────────────────────────

export type ConversationStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING' | 'RESOLVED' | 'ARCHIVED';
export type ConversationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type ConversationSource = 'EMAIL' | 'MANUAL' | 'AGENT';
export type SenderType = 'USER' | 'AGENT' | 'CUSTOMER';

export interface ConversationResponse {
  id: string;
  subject: string;
  customerId: string | null;
  status: ConversationStatus;
  priority: ConversationPriority;
  slaDueAt: string | null;
  assignedTo: string | null;
  source: ConversationSource;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConversationRequest {
  subject: string;
  customerId?: string;
  priority?: ConversationPriority;
  source?: ConversationSource;
}

export interface MessageResponse {
  id: string;
  conversationId: string;
  content: string;
  senderType: SenderType;
  senderId: string | null;
  sentAt: string;
}

export interface MessageCreateRequest {
  content: string;
  senderType: SenderType;
  senderId?: string;
}

export interface ConversationLinkResponse {
  id: string;
  conversationId: string;
  linkedType: string;
  linkedId: string;
  createdAt: string;
}

// ─── Customers ──────────────────────────────────────────

export type AddressType = 'BILLING' | 'SHIPPING' | 'BOTH';

export interface CustomerResponse {
  id: string;
  companyName: string;
  taxId: string | null;
  customerNumber: string | null;
  shortName: string | null;
  status: string;
  contacts: ContactResponse[];
  addresses: AddressResponse[];
  priceGroups: PriceGroupResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ContactResponse {
  id: string;
  customerId: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  position: string | null;
  isPrimary: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResponse {
  id: string;
  customerId: string;
  type: AddressType;
  street: string;
  zip: string;
  city: string;
  country: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PriceGroupResponse {
  id: string;
  customerId: string;
  name: string;
  discountPercent: number;
  validFrom: string;
  validUntil: string | null;
  createdAt: string;
  updatedAt: string;
}

// ─── Documents ──────────────────────────────────────────

export type DocumentStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface DocumentResponse {
  id: string;
  title: string;
  description: string | null;
  category: string | null;
  categoryId: string | null;
  excerpt: string | null;
  fileKey: string;
  fileName: string;
  mimeType: string | null;
  fileSizeBytes: number | null;
  version: number;
  status: DocumentStatus;
  uploadedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

// ─── Knowledge ──────────────────────────────────────────

export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface KnowledgeCategoryResponse {
  id: string;
  name: string;
  color: string | null;
  createdAt: string;
}

export interface KnowledgeTagResponse {
  id: string;
  name: string;
}

export interface KnowledgeArticleResponse {
  id: string;
  title: string;
  slug: string;
  content: string | null;
  excerpt: string | null;
  status: ArticleStatus;
  categoryId: string | null;
  categoryName: string | null;
  categoryColor: string | null;
  authorId: string | null;
  authorName: string | null;
  tags: KnowledgeTagResponse[];
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeArticleSummaryResponse {
  id: string;
  title: string;
  slug: string;
  excerpt: string | null;
  status: ArticleStatus;
  categoryId: string | null;
  categoryName: string | null;
  categoryColor: string | null;
  authorName: string | null;
  tags: KnowledgeTagResponse[];
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeSearchResultResponse {
  type: 'article' | 'document';
  id: string;
  title: string;
  excerpt: string | null;
  category: string | null;
  updatedAt: string;
}

export interface DocumentLinkResponse {
  id: string;
  documentId: string;
  linkedType: string;
  linkedId: string;
  createdAt: string;
}

// ─── BOM ────────────────────────────────────────────────

export type PartType = 'PRODUCT' | 'COMPONENT' | 'RAW_MATERIAL';
export type VersionStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export interface PartResponse {
  id: string;
  partNumber: string;
  name: string;
  description: string | null;
  type: PartType;
  unitId: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CalculationResponse {
  id: string;
  partId: string;
  bomVersionId: string;
  processPlanId: string;
  quantity: number;
  materialCost: number;
  laborCost: number;
  overheadCost: number;
  totalCost: number;
  currency: string;
  calculatedAt: string;
  calculatedBy: string | null;
}

export interface JobCalculationResponse {
  id: string;
  jobId: string;
  calculationId: string;
  actualMaterialCost: number | null;
  actualLaborCost: number | null;
  actualTotalCost: number | null;
  variancePercent: number | null;
  finalizedAt: string | null;
}

// ─── BOM Request Types ─────────────────────────────────

export interface CreatePartRequest {
  partNumber: string;
  name: string;
  description?: string;
  type: string;
  unitId?: string;
  status?: string;
}

export interface UpdatePartRequest {
  name?: string;
  description?: string;
  type?: string;
  unitId?: string;
  status?: string;
}

export interface BomVersionResponse {
  id: string;
  partId: string;
  versionNumber: number;
  status: VersionStatus;
  validFrom: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface CreateBomVersionRequest {
  partId: string;
  versionNumber?: number;
  validFrom?: string;
  createdBy?: string;
}

export interface BomItemResponse {
  id: string;
  bomVersionId: string;
  componentPartId: string;
  quantity: number;
  unitId: string | null;
  position: number;
  notes: string | null;
}

export interface BomItemRequest {
  componentPartId: string;
  quantity: number;
  unitId?: string;
  position: number;
  notes?: string;
}

export interface ProcessPlanResponse {
  id: string;
  partId: string;
  versionNumber: number;
  name: string;
  status: VersionStatus;
  validFrom: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface CreateProcessPlanRequest {
  partId: string;
  versionNumber?: number;
  name: string;
  validFrom?: string;
  createdBy?: string;
}

export interface ProcessStepResponse {
  id: string;
  processPlanId: string;
  stepNumber: number;
  name: string;
  description: string | null;
  stationId: string | null;
  machineId: string | null;
  setupTimeMinutes: number;
  processingTimeMinutes: number;
  notes: string | null;
}

export interface ProcessStepRequest {
  stepNumber: number;
  name: string;
  description?: string;
  stationId?: string;
  machineId?: string;
  setupTimeMinutes: number;
  processingTimeMinutes: number;
  notes?: string;
}

export interface CalculateRequest {
  partId: string;
  bomVersionId: string;
  processPlanId: string;
  quantity: number;
  calculatedBy?: string;
}

export interface CostRateResponse {
  id: string;
  rateType: string;
  entityId: string;
  ratePerHour: number;
  validFrom: string;
  validUntil: string | null;
}

// ─── Role Agent Defaults ────────────────────────────────

export interface RoleAgentDefaultResponse {
  id: string;
  role: string;
  agentInstanceId: string;
  agentInstanceName: string;
}

export interface RoleAgentDefaultUpdateRequest {
  role: string;
  agentInstanceId: string;
}

// ─── System Admin ──────────────────────────────────────
export type CompanyPlan = 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
export type CompanyStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED';

export interface CompanyResponse {
  id: string;
  name: string;
  slug: string;
  plan: CompanyPlan;
  status: CompanyStatus;
  active: boolean;
  createdAt: string;
  suspendedAt: string | null;
  suspendReason: string | null;
}

export interface CompanyCreateRequest {
  name: string;
  slug: string;
  plan?: CompanyPlan;
  adminEmail: string;
  adminFirstName: string;
  adminLastName: string;
  adminPassword?: string;
}

export interface CompanyCreateResponse {
  id: string;
  name: string;
  slug: string;
  plan: string;
  adminEmail: string;
  adminPassword: string;
}

export interface CompanyUpdateRequest {
  name?: string;
  plan?: CompanyPlan;
}

export interface CompanyStatsResponse {
  userCount: number;
  activeAgentRuns30d: number;
  storageUsedMb: number;
  lastActiveAt: string | null;
}

export interface CompanyAdminResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface PasswordResetResponse {
  userId: string;
  newPassword: string;
}

// ─── Events ─────────────────────────────────────────────

export interface DomainEventResponse {
  id: string;
  eventType: string;
  sourceType: string;
  sourceId: string | null;
  payload: string | null;
  processed: boolean;
  createdAt: string;
}

export interface ScheduledTriggerResponse {
  id: string;
  instanceId: string;
  cronExpression: string;
  lastRunAt: string | null;
  nextRunAt: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// ─── Tenant Config ─────────────────────────────────────

export interface TenantConfigResponse {
  id: string;
  name: string;
  slug: string;
  plan: string;
  status: string;
  logoUrl: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  address: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  website: string | null;
  vatId: string | null;
}

export interface TenantConfigUpdateRequest {
  name?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  website?: string;
  vatId?: string;
}

// ─── Budget ────────────────────────────────────────────

export interface BudgetOverviewResponse {
  currentMonth: BudgetPeriod;
  last30Days: BudgetPeriod;
  dailyUsage: DailyUsage[];
}

export interface BudgetPeriod {
  totalCostUsd: number;
  totalTokens: number;
  totalRuns: number;
  byAgent: AgentBudget[];
}

export interface AgentBudget {
  agentName: string;
  costUsd: number;
  tokens: number;
  runs: number;
}

export interface DailyUsage {
  date: string;
  costUsd: number;
  tokens: number;
}

// ─── Notification Settings ─────────────────────────────

export interface NotificationSettingsResponse {
  agentRunCompleted: boolean;
  agentRunFailed: boolean;
  stockBelowMinimum: boolean;
  machineIncident: boolean;
  jobOverdue: boolean;
  absenceRequest: boolean;
  inboxNewMessage: boolean;
  inApp: boolean;
  emailNotifications: boolean;
}

export type NotificationSettingsUpdateRequest = NotificationSettingsResponse;

// ─── Available Tools ───────────────────────────────────

export interface AvailableToolResponse {
  name: string;
  description: string;
  permissionLevel: string;
}

// ─── Chat ──────────────────────────────────────────────

export interface ChatSessionResponse {
  id: string;
  title: string | null;
  agentInstanceId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessageResponse {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

// ─── Agent Template Detail ─────────────────────────────

export interface AgentTemplateDetailResponse {
  id: string;
  name: string;
  role: string;
  description: string;
  basePrompt: string;
  allowedTools: string;
  triggerTypes: string;
  maxTokensPerRun: number;
  dailyTokenBudget: number;
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}
