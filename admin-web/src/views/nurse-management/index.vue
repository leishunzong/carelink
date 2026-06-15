<template>
  <div class="nurse-management">
    <h2 class="page-title">护工管理</h2>
    
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-input v-model="searchForm.realName" placeholder="护工姓名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.phone" placeholder="手机号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchForm.gender" placeholder="性别" clearable style="width: 120px">
          <el-option label="男" :value="1" />
          <el-option label="女" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="searchForm.minAge" placeholder="最小年龄" clearable controls-position="right" :min="0" :max="100" style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="searchForm.maxAge" placeholder="最大年龄" clearable controls-position="right" :min="0" :max="100" style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.education" placeholder="学历" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="searchForm.workYears" placeholder="从业年限" clearable controls-position="right" :min="0" style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.cityName" placeholder="服务城市" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchForm.workState" placeholder="工作状态" clearable style="width: 150px">
          <el-option label="接单中" :value="1" />
          <el-option label="服务中" :value="2" />
          <el-option label="休息中" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="birthday" label="出生日期" width="110" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="ethnicity" label="民族" width="100" />
      <el-table-column prop="zodiac" label="属相" width="100" />
      <el-table-column prop="nativePlace" label="籍贯" width="120" />
      <el-table-column prop="education" label="学历" width="100" />
      <el-table-column prop="workYears" label="从业年限" width="100">
        <template #default="{ row }">
          {{ row.workYears }}年
        </template>
      </el-table-column>
      <el-table-column prop="cityName" label="服务城市" width="120" />
      <el-table-column prop="residentAddress" label="居住地址" width="200" show-overflow-tooltip />
      <el-table-column prop="workState" label="工作状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.workState === 1" type="success">接单中</el-tag>
          <el-tag v-else-if="row.workState === 2" type="warning">服务中</el-tag>
          <el-tag v-else-if="row.workState === 3" type="info">休息中</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="verifyStatus" label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.verifyStatus === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.verifyStatus === 1" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已拒绝</el-tag>
        </template>
      </el-table-column>
      
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
    <el-dialog v-model="detailVisible" title="护工详情" width="800px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="姓名">{{ currentRow.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ currentRow.gender === 1 ? '男' : currentRow.gender === 2 ? '女' : '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ currentRow.birthday }}</el-descriptions-item>
        <el-descriptions-item label="民族">{{ currentRow.ethnicity }}</el-descriptions-item>
        <el-descriptions-item label="属相">{{ currentRow.zodiac }}</el-descriptions-item>
        <el-descriptions-item label="籍贯">{{ currentRow.nativePlace }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ currentRow.education }}</el-descriptions-item>
        <el-descriptions-item label="从业年限">{{ currentRow.workYears }}年</el-descriptions-item>
        <el-descriptions-item label="服务城市">{{ currentRow.cityName }}</el-descriptions-item>
        <el-descriptions-item label="居住地址" :span="2">{{ currentRow.residentAddress }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag v-if="currentRow.verifyStatus === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="currentRow.verifyStatus === 1" type="success">已通过</el-tag>
          <el-tag v-else type="danger">已拒绝</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCaregiverList, type CaregiverListItem } from '@/api/admin'

const loading = ref(false)
const searchForm = reactive({ realName: '', phone: '', gender: undefined as number | undefined })
const tableData = ref<CaregiverListItem[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<CaregiverListItem | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCaregiverList({
      realName: searchForm.realName || undefined,
      phone: searchForm.phone || undefined,
      gender: searchForm.gender,
      minAge: searchForm.minAge,
      maxAge: searchForm.maxAge,
      education: searchForm.education || undefined,
      workYears: searchForm.workYears,
      cityName: searchForm.cityName || undefined,
      workState: searchForm.workState,
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
  searchForm.realName = ''
  searchForm.phone = ''
  searchForm.gender = undefined
  pagination.current = 1
  fetchData()
}

const handleView = (row: CaregiverListItem) => {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.nurse-management {
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
