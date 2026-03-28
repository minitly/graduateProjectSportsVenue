export const STATUS_TEXT_MAP = {
  // 场地状态
  AVAILABLE: '可预约',
  MAINTAIN: '维护中',
  SUSPEND: '暂停预约',
  DISABLED: '已停用',

  // 公告状态
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下线',

  // 场地预约状态
  APPLIED: '预约成功',
  CANCELED: '已取消',
  VERIFIED: '已核销',
  VIOLATION: '违规',

  // 器材借用状态
  REQUESTED: '已申请',
  USING: '使用中',
  RETURNED: '已归还',

  // 器材状态（如有展示）
  GOOD: '完好',
  DAMAGED: '损坏',
  LOST: '丢失'
}

export function getStatusText(status, fallback = '未知状态') {
  if (!status) return fallback
  return STATUS_TEXT_MAP[status] || status
}
