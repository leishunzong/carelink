<template>
  <div class="user-review">
    <h2 class="page-title">用户评价</h2>
    
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-input v-model="searchForm.nickname" placeholder="用户昵称" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.caregiverName" placeholder="护工姓名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.orderNo" placeholder="订单号" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%" header-align="center">
      <el-table-column prop="orderNo" label="订单号" width="180" align="center" />
      <el-table-column prop="caregiverName" label="护工姓名" width="120" align="center" />
      <el-table-column prop="nickname" label="用户昵称" width="120" align="center" />
      <el-table-column prop="stars" label="评分" width="150" align="center">
        <template #default="{ row }">
          <el-rate v-model="row.stars" disabled text-color="#ff9900" />
        </template>
      </el-table-column>
      <el-table-column prop="tags" label="评价标签" width="250" align="center">
        <template #default="{ row }">
          <div class="tag-container">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 5px; margin-bottom: 5px">
              {{ tag }}
            </el-tag>
            <span v-if="!row.tags || row.tags.length === 0">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="250" show-overflow-tooltip align="center" />
      <el-table-column prop="createTime" label="评价时间" width="170" align="center" />
      
      <el-table-column label="操作" fixed="right" width="100" align="center">
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
    <el-dialog v-model="detailVisible" title="评价详情" width="700px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="护工姓名">{{ currentRow.caregiverName }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ currentRow.nickname }}</el-descriptions-item>
        <el-descriptions-item label="评分" :span="2">
          <el-rate v-model="currentRow.stars" disabled text-color="#ff9900" />
        </el-descriptions-item>
        <el-descriptions-item label="评价标签" :span="2">
          <el-tag v-for="tag in currentRow.tags" :key="tag" size="small" style="margin-right: 5px">
            {{ tag }}
          </el-tag>
          <span v-if="!currentRow.tags || currentRow.tags.length === 0">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="2">{{ currentRow.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getReviewList, type ReviewListItem } from '@/api/admin'

const loading = ref(false)
const searchForm = reactive({ caregiverName: '', nickname: '', stars: undefined as number | undefined })
const tableData = ref<ReviewListItem[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<ReviewListItem | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getReviewList({
      caregiverName: searchForm.caregiverName || undefined,
      nickname: searchForm.nickname || undefined,
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
  searchForm.nickname = ''
  searchForm.caregiverName = ''
  searchForm.orderNo = ''
  pagination.current = 1
  fetchData()
}

const handleView = (row: ReviewListItem) => {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.user-review {
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
  
  .tag-container {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    line-height: 1.5;
  }
}
</style>
