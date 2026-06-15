<template>
  <div class="admission-review-page">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="护工姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入护工姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="birthday" label="出生日期" width="110" />
        <el-table-column prop="education" label="学历" width="100" />
        <el-table-column prop="workYears" label="从业年限" width="90">
          <template #default="{ row }">
            {{ row.workYears }}年
          </template>
        </el-table-column>
        <el-table-column prop="cityName" label="服务城市" width="120" />
        <el-table-column prop="verifyStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.verifyStatus === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.verifyStatus === 1" type="success">已通过</el-tag>
            <el-tag v-else type="danger">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.verifyStatus === 0" type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.verifyStatus === 0" type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="入驻申请详情" width="800px">
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
        <el-descriptions-item v-if="currentRow.rejectReason" label="拒绝原因" :span="2">
          {{ currentRow.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="currentRow" class="material-images">
        <h3>审核材料</h3>
        <div class="image-list">
          <div class="image-item">
            <p>头像</p>
            <el-image :src="currentRow.avatar" :preview-src-list="[currentRow.avatar]" fit="cover" />
          </div>
          <div 
            v-for="material in currentRow.verifyMaterials" 
            :key="material.materialType + '-' + material.sortOrder"
            class="image-item"
          >
            <p>{{ material.materialTypeName }}</p>
            <el-image 
              :src="material.fileUrl" 
              :preview-src-list="currentRow.verifyMaterials.map(m => m.fileUrl)" 
              fit="cover" 
            />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="500px">
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef">
        <el-form-item label="拒绝原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject" :loading="rejectLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getSettleList, auditSettle, type SettleListItem } from '@/api/admin'

// 搜索表单
const searchForm = reactive({
  realName: '',
  phone: ''
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 表格数据
const tableData = ref<SettleListItem[]>([])
const loading = ref(false)

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<SettleListItem | null>(null)

// 拒绝对话框
const rejectVisible = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectLoading = ref(false)
const rejectForm = reactive({
  rejectReason: ''
})
const rejectRules: FormRules = {
  rejectReason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' }
  ]
}

// 查询数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSettleList({
      realName: searchForm.realName || undefined,
      phone: searchForm.phone || undefined,
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

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.realName = ''
  searchForm.phone = ''
  pagination.current = 1
  fetchData()
}

// 查看详情
const handleView = (row: SettleListItem) => {
  currentRow.value = row
  detailVisible.value = true
}

// 通过审核
const handleApprove = async (row: SettleListItem) => {
  try {
    await ElMessageBox.confirm('确认通过该护工的入驻申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await auditSettle({
      caregiverId: row.id,
      passed: true
    })
    
    ElMessage.success('审核通过')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 拒绝审核
const handleReject = (row: SettleListItem) => {
  currentRow.value = row
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectFormRef.value || !currentRow.value) return
  
  await rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      rejectLoading.value = true
      try {
        await auditSettle({
          caregiverId: currentRow.value!.id,
          passed: false,
          rejectReason: rejectForm.rejectReason
        })
        
        ElMessage.success('已拒绝')
        rejectVisible.value = false
        fetchData()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        rejectLoading.value = false
      }
    }
  })
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.admission-review-page {
  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }

  .material-images {
    margin-top: 20px;

    h3 {
      font-size: 16px;
      margin-bottom: 15px;
    }

    .image-list {
      display: flex;
      gap: 20px;
      flex-wrap: wrap;

      .image-item {
        text-align: center;

        p {
          margin-bottom: 8px;
          color: #666;
          font-size: 14px;
        }

        .el-image {
          width: 150px;
          height: 150px;
          border: 1px solid #ddd;
          border-radius: 4px;
        }
      }
    }
  }
}
</style>
