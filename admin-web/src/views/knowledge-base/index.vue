<template>
  <div class="knowledge-base">
    <h2 class="page-title">知识库管理</h2>
    
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAddText">新增文档</el-button>
      <el-upload
        action="#"
        :before-upload="handleFileUpload"
        :show-file-list="false"
        accept=".txt,.md,.pdf,.doc,.docx"
        style="display: inline-block"
      >
        <el-button type="success" :icon="Upload" :loading="uploadLoading">上传文档</el-button>
      </el-upload>
    </div>

    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-input v-model="searchForm.title" placeholder="文档标题/内容" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column prop="title" label="文档标题" min-width="250" show-overflow-tooltip />
      <el-table-column prop="content" label="文档内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createTime" label="上传时间" width="170" />
      
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增文档弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增文档" width="700px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="文档内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="10"
            placeholder="请输入文档内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看文档详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="文档详情" width="800px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="文档标题">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="文件名" v-if="currentRow.fileName">{{ currentRow.fileName }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="文档内容">
          <div class="document-content">{{ currentRow.content || '暂无内容' }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getRagDocuments, uploadRagDocument, addRagDocumentText, deleteRagDocument, type RagDocument } from '@/api/admin'

const loading = ref(false)
const uploadLoading = ref(false)
const searchForm = reactive({ title: '' })
const tableData = ref<RagDocument[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 查看文档详情
const viewDialogVisible = ref(false)
const currentRow = ref<RagDocument | null>(null)

const formData = reactive({ title: '', content: '' })

const formRules: FormRules = {
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文档内容', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getRagDocuments()
    // 根据搜索条件过滤
    if (searchForm.title) {
      tableData.value = data.filter(item => 
        item.title.includes(searchForm.title) || 
        (item.content && item.content.includes(searchForm.title))
      )
    } else {
      tableData.value = data
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  searchForm.title = ''
  fetchData()
}

const handleAddText = () => {
  Object.assign(formData, { title: '', content: '' })
  dialogVisible.value = true
}

const handleView = (row: RagDocument) => {
  currentRow.value = row
  viewDialogVisible.value = true
}

const handleFileUpload = async (file: File) => {
  uploadLoading.value = true
  try {
    const formDataObj = new FormData()
    formDataObj.append('file', file)
    
    await uploadRagDocument(formDataObj)
    ElMessage.success('文档上传成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
  return false // 阻止默认上传行为
}

const handleDelete = async (row: RagDocument) => {
  try {
    await ElMessageBox.confirm(`确认删除该文档吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deleteRagDocument(row.id)
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
        await addRagDocumentText({
          title: formData.title,
          content: formData.content
        })
        ElMessage.success('提交成功')
        dialogVisible.value = false
        fetchData()
      } catch (error: any) {
        ElMessage.error(error.message || '提交失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.knowledge-base {
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
    display: flex;
    gap: 10px;
  }
  .search-form {
    margin-bottom: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 4px;
  }
  .document-content {
    max-height: 400px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.6;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
  }
}
</style>
