<template>
  <div class="order-management">
    <h2 class="page-title">订单管理</h2>
    
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-input v-model="searchForm.orderNo" placeholder="订单号" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchForm.orderType" placeholder="订单类型" clearable style="width: 150px">
          <el-option label="系统匹配" :value="1" />
          <el-option label="定向预约" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchForm.status" placeholder="订单状态" clearable style="width: 150px">
          <el-option label="待支付" :value="1" />
          <el-option label="待接单" :value="2" />
          <el-option label="待上门" :value="3" />
          <el-option label="服务中" :value="4" />
          <el-option label="待确认" :value="5" />
          <el-option label="已完成" :value="6" />
          <el-option label="已取消" :value="7" />
          <el-option label="已关闭" :value="8" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.caregiverName" placeholder="护工名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.contactName" placeholder="联系人姓名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.cityName" placeholder="城市名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="orderType" label="订单类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.orderType === 1 ? 'primary' : 'success'">
            {{ row.orderType === 1 ? '系统匹配' : '定向预约' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="caregiverName" label="护工名" width="120" />
      <el-table-column prop="packageName" label="服务包名称" width="150" />
      <el-table-column prop="detailAddress" label="详细地址" width="200" show-overflow-tooltip />
      <el-table-column prop="billingMethod" label="计费方式" width="100">
        <template #default="{ row }">
          {{ getBillingMethodText(row.billingMethod) }}
        </template>
      </el-table-column>
      <el-table-column prop="buyQuantity" label="购买数量" width="100" />
      <el-table-column prop="unitPrice" label="单价" width="100">
        <template #default="{ row }">
          ¥{{ row.unitPrice }}
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="订单金额" width="120">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: 600">¥{{ row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="expectStartTime" label="预约时间" width="170" />
      <el-table-column prop="status" label="订单状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      
      <el-table-column label="操作" fixed="right" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleView(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchData"
      @current-change="fetchData"
      class="pagination"
    />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">
          <el-tag :type="currentRow.orderType === 1 ? 'primary' : 'success'">
            {{ currentRow.orderType === 1 ? '系统匹配' : '定向预约' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="护工名">{{ currentRow.caregiverName || '待分配' }}</el-descriptions-item>
        <el-descriptions-item label="服务包名称">{{ currentRow.packageName }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ currentRow.detailAddress }}</el-descriptions-item>
        <el-descriptions-item label="计费方式">{{ getBillingMethodText(currentRow.billingMethod) }}</el-descriptions-item>
        <el-descriptions-item label="购买数量">{{ currentRow.buyQuantity }}</el-descriptions-item>
        <el-descriptions-item label="单价">¥{{ currentRow.unitPrice }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ currentRow.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="预约时间" :span="2">{{ currentRow.expectStartTime }}</el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">{{ currentRow.realStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">{{ currentRow.finishTime || '未完成' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getOrderList, type OrderListItem } from '@/api/admin'

const loading = ref(false)
const searchForm = reactive({ 
  orderNo: '', 
  orderType: undefined as number | undefined,
  status: undefined as number | undefined,
  caregiverName: '',
  contactName: '',
  cityName: ''
})
const tableData = ref<OrderListItem[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<OrderListItem | null>(null)

const getStatusType = (status: number) => {
  const typeMap: Record<number, any> = {
    1: 'warning',    // 待支付
    2: 'info',       // 待接单
    3: 'info',       // 待上门
    4: 'primary',    // 服务中
    5: 'warning',    // 待确认
    6: 'success',    // 已完成
    7: 'danger',     // 已取消
    8: 'info'        // 已关闭
  }
  return typeMap[status] || ''
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '待支付',
    2: '待接单',
    3: '待上门',
    4: '服务中',
    5: '待确认',
    6: '已完成',
    7: '已取消',
    8: '已关闭'
  }
  return textMap[status] || '未知'
}

const getBillingMethodText = (method: number) => {
  const textMap: Record<number, string> = {
    1: '按月',
    2: '按天',
    3: '按小时',
    4: '按次'
  }
  return textMap[method] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getOrderList({
      orderNo: searchForm.orderNo || undefined,
      orderType: searchForm.orderType,
      status: searchForm.status,
      caregiverName: searchForm.caregiverName || undefined,
      contactName: searchForm.contactName || undefined,
      cityName: searchForm.cityName || undefined,
      current: pagination.current,
      size: pagination.size
    })
    tableData.value = res.records
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.orderType = undefined
  searchForm.status = undefined
  searchForm.caregiverName = ''
  searchForm.contactName = ''
  searchForm.cityName = ''
  pagination.current = 1
  fetchData()
}

const handleView = (row: OrderListItem) => {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.order-management {
  .page-title {
    font-size: 20px;
    margin-bottom: 24px;
    color: #333;
  }
  .search-form {
    margin-bottom: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 4px;
  }
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
