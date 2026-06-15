<template>
  <div class="skill-management">
    <h2 class="page-title">技能管理</h2>
    
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增技能</el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="skillName" label="技能名称" width="150" />
      <el-table-column prop="skillType" label="技能类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ getSkillTypeName(row.skillType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="技能描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="needAudit" label="是否需要审核" width="150">
        <template #default="{ row }">
          <el-tag :type="row.needAudit ? 'success' : 'info'">
            {{ row.needAudit ? '需要审核' : '无需审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      
      <el-table-column label="操作" fixed="right" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="技能名称" prop="skillName">
          <el-input v-model="formData.skillName" placeholder="请输入技能名称" />
        </el-form-item>
        <el-form-item label="技能类型" prop="skillType">
          <el-select v-model="formData.skillType" placeholder="请选择技能类型" style="width: 100%">
            <el-option label="生活护理" :value="1" />
            <el-option label="医疗护理" :value="2" />
            <el-option label="康复护理" :value="3" />
            <el-option label="心理护理" :value="4" />
            <el-option label="专业技能" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能描述" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入技能描述" 
          />
        </el-form-item>
        <el-form-item label="是否需要审核" prop="needAudit">
          <el-radio-group v-model="formData.needAudit">
            <el-radio :label="1">需要审核</el-radio>
            <el-radio :label="0">无需审核</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getSkillPage, addSkill, updateSkill, deleteSkill, type SkillDict } from '@/api/admin'

const loading = ref(false)
const tableData = ref<SkillDict[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增技能')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({ 
  id: undefined as number | undefined, 
  skillName: '', 
  skillType: 1 as 1 | 2 | 3 | 4 | 5 | 6,
  description: '',
  needAudit: 1 as 0 | 1
})

const formRules: FormRules = {
  skillName: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  skillType: [{ required: true, message: '请选择技能类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入技能描述', trigger: 'blur' }]
}

// 技能类型映射
const getSkillTypeName = (type: number) => {
  const map: Record<number, string> = {
    1: '生活护理',
    2: '医疗护理',
    3: '康复护理',
    4: '心理护理',
    5: '专业技能',
    6: '其他'
  }
  return map[type] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSkillPage({
      current: 1,
      size: 100
    })
    tableData.value = res.records
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增技能'
  Object.assign(formData, { id: undefined, skillName: '', skillType: 1, description: '', needAudit: 1 })
  // 清理表单验证
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  dialogVisible.value = true
}

const handleEdit = (row: SkillDict) => {
  dialogTitle.value = '编辑技能'
  Object.assign(formData, { 
    id: row.id, 
    skillName: row.skillName, 
    skillType: row.skillType,
    description: row.description,
    needAudit: row.needAudit 
  })
  // 清理表单验证
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  dialogVisible.value = true
}

const handleDialogClose = () => {
  // 对话框关闭时清理表单验证
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const handleDelete = async (row: SkillDict) => {
  try {
    await ElMessageBox.confirm(`确认删除技能"${row.skillName}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deleteSkill(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (formData.id) {
          await updateSkill(formData.id, { 
            skillName: formData.skillName, 
            skillType: formData.skillType,
            description: formData.description,
            needAudit: formData.needAudit 
          })
          ElMessage.success('编辑成功')
        } else {
          await addSkill({ 
            skillName: formData.skillName, 
            skillType: formData.skillType,
            description: formData.description,
            needAudit: formData.needAudit 
          })
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.skill-management {
  .page-title {
    font-size: 20px;
    margin-bottom: 24px;
    color: #333;
  }
  .toolbar {
    margin-bottom: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 4px;
  }
}
</style>
