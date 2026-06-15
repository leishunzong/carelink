<template>
  <div class="service-package">
    <h2 class="page-title">服务包管理</h2>
    
    <el-form :model="searchForm" inline class="search-form">
      <el-form-item>
        <el-select v-model="searchForm.category" placeholder="服务类型" clearable style="width: 150px">
          <el-option label="居家陪护" :value="1" />
          <el-option label="医院陪护" :value="2" />
          <el-option label="周期护理" :value="3" />
          <el-option label="家政服务" :value="4" />
          <el-option label="陪诊服务" :value="5" />
          <el-option label="母婴护理" :value="6" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增服务包</el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe border style="width: 100%">
      <el-table-column label="封面图片" width="100">
        <template #default="{ row }">
          <el-image 
            v-if="row.coverImage"
            :src="row.coverImage" 
            :preview-src-list="[row.coverImage]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px"
          />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="服务包名称" width="150" />
      <el-table-column prop="category" label="服务类型" width="120">
        <template #default="{ row }">
          {{ getCategoryText(row.category) }}
        </template>
      </el-table-column>
      <el-table-column label="计费方式" width="220">
        <template #default="{ row }">
          <div>
            <el-tag v-if="row.allowMonth" size="small" style="margin: 2px">月:¥{{ row.priceMonth }}</el-tag>
            <el-tag v-if="row.allowDay" size="small" style="margin: 2px">天:¥{{ row.priceDay }}</el-tag>
            <el-tag v-if="row.allowHour" size="small" style="margin: 2px">时:¥{{ row.priceHour }}</el-tag>
            <el-tag v-if="row.allowTimes" size="small" style="margin: 2px">次:¥{{ row.priceTimes }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="服务简介" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sales" label="销量" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" fixed="right" width="260">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
          <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
          <el-button 
            :type="row.status === 1 ? 'warning' : 'success'" 
            size="small" 
            link 
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="服务包名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入服务包名称" />
        </el-form-item>
        <el-form-item label="服务类型" prop="category">
          <el-select v-model="formData.category" placeholder="请选择服务类型" style="width: 100%">
            <el-option label="居家陪护" :value="1" />
            <el-option label="医院陪护" :value="2" />
            <el-option label="周期护理" :value="3" />
            <el-option label="家政服务" :value="4" />
            <el-option label="陪诊服务" :value="5" />
            <el-option label="母婴护理" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageChange"
            accept="image/*"
          >
            <template v-if="formData.coverImage">
              <div class="image-preview">
                <el-image 
                  :src="formData.coverImage" 
                  fit="cover"
                  style="width: 200px; height: 150px; border-radius: 4px"
                />
                <div class="image-actions">
                  <el-button size="small" type="primary" link @click.stop="handleReupload">重新上传</el-button>
                  <el-button size="small" type="danger" link @click.stop="handleRemoveImage">删除</el-button>
                </div>
              </div>
            </template>
            <template v-else>
              <el-button type="primary" :loading="imageUploading">
                <el-icon><Upload /></el-icon>
                {{ imageUploading ? '上传中...' : '点击上传封面图片' }}
              </el-button>
            </template>
          </el-upload>
          <div class="upload-tip">支持 jpg、png、gif 格式，建议尺寸 800x600</div>
        </el-form-item>
        <el-form-item label="服务简介" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入服务简介" />
        </el-form-item>
        <el-form-item label="服务详情" prop="detail">
          <el-input v-model="formData.detail" type="textarea" :rows="4" placeholder="请输入服务详情" />
        </el-form-item>
        <el-form-item label="计费方式">
          <el-checkbox-group v-model="billingMethods">
            <el-checkbox label="按月">按月</el-checkbox>
            <el-checkbox label="按天">按天</el-checkbox>
            <el-checkbox label="按小时">按小时</el-checkbox>
            <el-checkbox label="按次">按次</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="billingMethods.includes('按月')" label="按月单价">
          <el-input-number v-model="formData.priceMonth" :min="0" :precision="2" />
          <span style="margin-left: 10px">元/月</span>
        </el-form-item>
        <el-form-item v-if="billingMethods.includes('按天')" label="按天单价">
          <el-input-number v-model="formData.priceDay" :min="0" :precision="2" />
          <span style="margin-left: 10px">元/天</span>
        </el-form-item>
        <el-form-item v-if="billingMethods.includes('按小时')" label="按小时单价">
          <el-input-number v-model="formData.priceHour" :min="0" :precision="2" />
          <span style="margin-left: 10px">元/时</span>
        </el-form-item>
        <el-form-item v-if="billingMethods.includes('按次')" label="按次单价">
          <el-input-number v-model="formData.priceTimes" :min="0" :precision="2" />
          <span style="margin-left: 10px">元/次</span>
        </el-form-item>
        <el-form-item label="技能要求" prop="mandatorySkillIds">
          <el-select v-model="formData.mandatorySkillIds" multiple placeholder="请选择技能要求" style="width: 100%">
            <el-option v-for="skill in skillList" :key="skill.id" :label="skill.skillName" :value="skill.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="服务包详情" width="700px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="封面图片" :span="2">
          <el-image 
            v-if="currentRow.coverImage"
            :src="currentRow.coverImage" 
            :preview-src-list="[currentRow.coverImage]"
            fit="cover"
            style="width: 200px; height: 150px; border-radius: 4px"
          />
        </el-descriptions-item>
        <el-descriptions-item label="服务包名称">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">{{ getCategoryText(currentRow.category) }}</el-descriptions-item>
        <el-descriptions-item label="销量">{{ currentRow.sales }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentRow.status === 1 ? 'success' : 'info'">
            {{ currentRow.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务简介" :span="2">{{ currentRow.description }}</el-descriptions-item>
        <el-descriptions-item label="服务详情" :span="2">{{ currentRow.detail }}</el-descriptions-item>
        <el-descriptions-item label="计费方式" :span="2">
          <el-tag v-if="currentRow.allowMonth" size="small" style="margin-right: 8px">按月: ¥{{ currentRow.priceMonth }}</el-tag>
          <el-tag v-if="currentRow.allowDay" size="small" style="margin-right: 8px">按天: ¥{{ currentRow.priceDay }}</el-tag>
          <el-tag v-if="currentRow.allowHour" size="small" style="margin-right: 8px">按小时: ¥{{ currentRow.priceHour }}</el-tag>
          <el-tag v-if="currentRow.allowTimes" size="small">按次: ¥{{ currentRow.priceTimes }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="必备技能" :span="2">
          <el-tag v-for="skillId in currentRow.mandatorySkillIds" :key="skillId" size="small" style="margin-right: 8px">
            {{ getSkillName(skillId) }}
          </el-tag>
          <span v-if="!currentRow.mandatorySkillIds || currentRow.mandatorySkillIds.length === 0">无要求</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRow.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Upload } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { 
  getPackagePage, 
  addPackage, 
  updatePackage, 
  deletePackage, 
  onShelfPackage,
  offShelfPackage,
  getSkillPage,
  uploadImage,
  type ServicePackage 
} from '@/api/admin'

const loading = ref(false)
const searchForm = reactive({ 
  category: undefined as number | undefined, 
  status: undefined as number | undefined 
})
const tableData = ref<ServicePackage[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增服务包')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const skillList = ref<any[]>([])
const billingMethods = ref<string[]>([])
const imageUploading = ref(false)
const uploadRef = ref<any>(null)

// 详情对话框
const detailVisible = ref(false)
const currentRow = ref<ServicePackage | null>(null)

const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  category: 1,
  coverImage: '',
  description: '',
  detail: '',
  allowMonth: 0,
  allowDay: 0,
  allowHour: 0,
  allowTimes: 0,
  priceMonth: undefined as number | undefined,
  priceDay: undefined as number | undefined,
  priceHour: undefined as number | undefined,
  priceTimes: undefined as number | undefined,
  mandatorySkillIds: [] as number[]
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入服务包名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择服务类别', trigger: 'change' }],
  coverImage: [{ required: true, message: '请输入封面图片URL', trigger: 'blur' }],
  description: [{ required: true, message: '请输入服务简介', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入服务详情', trigger: 'blur' }]
}

const getCategoryText = (category: number) => {
  const categoryMap: Record<number, string> = {
    1: '居家陪护',
    2: '医院陪护',
    3: '周期护理',
    4: '家政服务',
    5: '陪诊服务',
    6: '母婴护理'
  }
  return categoryMap[category] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPackagePage({
      category: searchForm.category,
      status: searchForm.status,
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

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  searchForm.category = undefined
  searchForm.status = undefined
  fetchData()
}

const fetchSkillList = async () => {
  try {
    const res = await getSkillPage({
      current: 1,
      size: 100
    })
    skillList.value = res.records
  } catch (error) {
    console.error('获取技能列表失败', error)
  }
}

const getSkillName = (skillId: number) => {
  const skill = skillList.value.find(s => s.id === skillId)
  return skill ? skill.skillName : `技能ID: ${skillId}`
}

// 处理图片选择
const handleImageChange = async (file: UploadFile) => {
  if (!file.raw) return
  
  // 验证文件类型
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return
  }
  
  // 验证文件大小（限制5MB）
  const isLt5M = file.raw.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
    return
  }
  
  // 开始上传
  imageUploading.value = true
  try {
    const res = await uploadImage(file.raw)
    formData.coverImage = res.url
    ElMessage.success('图片上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

// 重新上传图片
const handleReupload = () => {
  // 触发上传组件点击事件
  const uploadElement = document.querySelector('.el-upload') as HTMLElement
  if (uploadElement) {
    uploadElement.click()
  }
}

// 删除图片
const handleRemoveImage = () => {
  formData.coverImage = ''
}


const handleView = (row: ServicePackage) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleAdd = () => {
  dialogTitle.value = '新增服务包'
  billingMethods.value = []
  Object.assign(formData, {
    id: undefined,
    name: '',
    category: 1,
    coverImage: '',
    description: '',
    detail: '',
    allowMonth: 0,
    allowDay: 0,
    allowHour: 0,
    allowTimes: 0,
    priceMonth: undefined,
    priceDay: undefined,
    priceHour: undefined,
    priceTimes: undefined,
    mandatorySkillIds: []
  })
  dialogVisible.value = true
}

const handleEdit = (row: ServicePackage) => {
  dialogTitle.value = '编辑服务包'
  
  // 设置计费方式复选框
  const methods: string[] = []
  if (row.allowMonth) methods.push('按月')
  if (row.allowDay) methods.push('按天')
  if (row.allowHour) methods.push('按小时')
  if (row.allowTimes) methods.push('按次')
  billingMethods.value = methods
  
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    category: row.category,
    coverImage: row.coverImage,
    description: row.description,
    detail: row.detail,
    allowMonth: row.allowMonth,
    allowDay: row.allowDay,
    allowHour: row.allowHour,
    allowTimes: row.allowTimes,
    priceMonth: row.priceMonth,
    priceDay: row.priceDay,
    priceHour: row.priceHour,
    priceTimes: row.priceTimes,
    mandatorySkillIds: row.mandatorySkillIds || []
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row: ServicePackage) => {
  const action = row.status === 1 ? '下架' : '上架'
  try {
    await ElMessageBox.confirm(`确认${action}服务包"${row.name}"吗？`, `确认${action}`, {
      type: 'warning'
    })
    
    if (row.status === 1) {
      await offShelfPackage(row.id)
    } else {
      await onShelfPackage(row.id)
    }
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

const handleDelete = async (row: ServicePackage) => {
  try {
    await ElMessageBox.confirm(`确认删除服务包"${row.name}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    await deletePackage(row.id)
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
        // 根据billingMethods设置allow字段
        const allowMonth = billingMethods.value.includes('按月') ? 1 : 0
        const allowDay = billingMethods.value.includes('按天') ? 1 : 0
        const allowHour = billingMethods.value.includes('按小时') ? 1 : 0
        const allowTimes = billingMethods.value.includes('按次') ? 1 : 0
        
        const submitData = {
          name: formData.name,
          category: formData.category,
          coverImage: formData.coverImage,
          description: formData.description,
          detail: formData.detail,
          allowMonth,
          allowDay,
          allowHour,
          allowTimes,
          priceMonth: allowMonth ? formData.priceMonth : undefined,
          priceDay: allowDay ? formData.priceDay : undefined,
          priceHour: allowHour ? formData.priceHour : undefined,
          priceTimes: allowTimes ? formData.priceTimes : undefined,
          mandatorySkillIds: formData.mandatorySkillIds
        }
        
        if (formData.id) {
          await updatePackage(formData.id, submitData)
          ElMessage.success('编辑成功')
        } else {
          await addPackage(submitData)
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

onMounted(() => {
  fetchData()
  fetchSkillList()
})
</script>

<style scoped lang="scss">
.service-package {
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
  .search-form {
    margin-bottom: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 4px;
  }
}

.image-preview {
  position: relative;
  display: inline-block;
  
  .image-actions {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
</style>
