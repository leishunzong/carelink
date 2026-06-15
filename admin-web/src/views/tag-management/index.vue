<template>
  <div class="tag-management">
    <h2 class="page-title">标签管理</h2>
    
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增标签</el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="name" label="标签名称" width="200" />
      <el-table-column prop="type" label="标签类型" width="150">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'success' : 'warning'">{{ row.type === 1 ? '好评' : row.type === 2 ? '差评' : '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      
      <el-table-column label="操作" fixed="right" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio :label="1">好评</el-radio>
            <el-radio :label="2">差评</el-radio>
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
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getTagList, addTag, updateTag, type ReviewTag } from '@/api/admin'

const loading = ref(false)
const tableData = ref<ReviewTag[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增标签')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive<{ 
  id?: number
  name: string
  type: 1 | 2
}>({ id: undefined, name: '', type: 1 })

const formRules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择标签类型', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTagList()
    tableData.value = res
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增标签'
  Object.assign(formData, { id: undefined, name: '', type: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: ReviewTag) => {
  dialogTitle.value = '编辑标签'
  Object.assign(formData, { id: row.id, name: row.name, type: row.type })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (formData.id) {
          // 编辑模式：使用 updateTag
          await updateTag(formData.id, { name: formData.name, type: formData.type })
          ElMessage.success('编辑成功')
        } else {
          // 新增模式：使用 addTag
          await addTag({ name: formData.name, type: formData.type })
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
.tag-management {
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
