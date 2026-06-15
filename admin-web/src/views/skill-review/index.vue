<template>
  <div class="skill-review">
    <h2 class="page-title">技能审核</h2>
    
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-input v-model="searchForm.caregiverName" placeholder="护工姓名" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.caregiverPhone" placeholder="手机号" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="searchForm.skillName" placeholder="技能名称" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="caregiverName" label="护工姓名" width="120" />
      <el-table-column prop="caregiverPhone" label="手机号" width="140" />
      <el-table-column prop="skillName" label="技能名称" width="150" />
      
      <el-table-column label="证书照片" width="150">
        <template #default="{ row }">
          <el-image
            :src="row.certImage"
            :preview-src-list="[row.certImage]"
            fit="cover"
            style="width: 80px; height: 50px"
          />
        </template>
      </el-table-column>
      
      <el-table-column prop="auditStatus" label="审核状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.auditStatus === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="row.auditStatus === 1" type="success">已通过</el-tag>
          <el-tag v-else-if="row.auditStatus === 2" type="danger">已拒绝</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="createTime" label="申请时间" width="170" />
      
      <el-table-column label="操作" fixed="right" width="200">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button v-if="row.auditStatus === 0" type="success" size="small" @click="handleApprove(row)">通过</el-button>
          <el-button v-if="row.auditStatus === 0" type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
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
    <el-dialog v-model="detailVisible" title="技能审核详情" width="600px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="护工姓名">{{ currentRow.caregiverName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.caregiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="技能名称">{{ currentRow.skillName }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag v-if="currentRow.auditStatus === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="currentRow.auditStatus === 1" type="success">已通过</el-tag>
          <el-tag v-else-if="currentRow.auditStatus === 2" type="danger">已拒绝</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.rejectReason" label="拒绝原因" :span="2">
          {{ currentRow.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="certificate-image">
        <h3>证书照片</h3>
        <el-image :src="currentRow?.certImage" :preview-src-list="[currentRow?.certImage]" fit="contain" style="width: 100%; max-height: 400px" />
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
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getSkillApplyList, auditSkillApply, type SkillApplyListItem } from '@/api/admin'

const loading = ref(false)
const searchForm = reactive({ caregiverName: '', caregiverPhone: '', skillName: '' })
const tableData = ref<SkillApplyListItem[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<SkillApplyListItem | null>(null)

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

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSkillApplyList({
      caregiverName: searchForm.caregiverName || undefined,
      caregiverPhone: searchForm.caregiverPhone || undefined,
      skillName: searchForm.skillName || undefined,
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
  searchForm.caregiverName = ''
  searchForm.caregiverPhone = ''
  searchForm.skillName = ''
  pagination.current = 1
  fetchData()
}

const handleView = (row: SkillApplyListItem) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleApprove = async (row: SkillApplyListItem) => {
  try {
    await ElMessageBox.confirm(`确认通过 ${row.caregiverName} 的技能审核吗？`, '确认通过', {
      type: 'success'
    })
    
    await auditSkillApply({
      caregiverSkillId: row.id,
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

const handleReject = (row: SkillApplyListItem) => {
  currentRow.value = row
  rejectForm.rejectReason = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectFormRef.value || !currentRow.value) return
  
  await rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      rejectLoading.value = true
      try {
        await auditSkillApply({
          caregiverSkillId: currentRow.value!.id,
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

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.skill-review {
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
