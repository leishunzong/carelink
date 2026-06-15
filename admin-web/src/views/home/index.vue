<template>
  <div class="operation-stats">
    <h2 class="page-title">运营数据</h2>
    
    <el-row :gutter="20">
      <!-- 第一行 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#1890ff"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.caregiverTotal }}</div>
              <div class="stat-label">护工</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#52c41a"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.orderTotal }}</div>
              <div class="stat-label">订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#faad14"><UserFilled /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userTotal }}</div>
              <div class="stat-label">用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#f5222d"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reviewTotal }}</div>
              <div class="stat-label">评价</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 第二行 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#722ed1"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingSettleTotal }}</div>
              <div class="stat-label">待审核的护工入驻</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#eb2f96"><Medal /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingSkillTotal }}</div>
              <div class="stat-label">待审核的技能</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#13c2c2"><Box /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.servicePackageTotal }}</div>
              <div class="stat-label">服务包</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#2f54eb"><DataAnalysis /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.skillTotal }}</div>
              <div class="stat-label">技能</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 第三行 -->
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#fa8c16"><PriceTag /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reviewTagTotal }}</div>
              <div class="stat-label">评价标签</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#52c41a"><Reading /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.ragDocumentTotal }}</div>
              <div class="stat-label">知识库</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#faad14"><Money /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ Math.round(stats.todayRevenue) }}<span class="unit">元</span></div>
              <div class="stat-label">今日营收</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#f5222d"><TrendCharts /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ Math.round(stats.totalRevenue) }}<span class="unit">元</span></div>
              <div class="stat-label">累计营收</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  User, UserFilled, Document, ChatDotRound, Warning, 
  Medal, Box, DataAnalysis, PriceTag, Reading, Money, TrendCharts
} from '@element-plus/icons-vue'
import { getOperationStats, type OperationStats } from '@/api/admin'

const stats = ref<OperationStats>({
  caregiverTotal: 0,
  orderTotal: 0,
  pendingSettleTotal: 0,
  pendingSkillTotal: 0,
  userTotal: 0,
  reviewTotal: 0,
  servicePackageTotal: 0,
  skillTotal: 0,
  reviewTagTotal: 0,
  ragDocumentTotal: 0,
  todayRevenue: 0,
  totalRevenue: 0
})

const fetchStats = async () => {
  try {
    const data = await getOperationStats()
    stats.value = data
  } catch (error) {
    ElMessage.error('获取运营数据失败')
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped lang="scss">
.operation-stats {
  .page-title {
    font-size: 20px;
    margin-bottom: 24px;
    color: #333;
    font-weight: 600;
  }

  .stat-card {
    height: 140px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    }

    :deep(.el-card__body) {
      height: 100%;
      display: flex;
      align-items: center;
      padding: 24px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 20px;
      width: 100%;

      .stat-icon {
        font-size: 52px;
        flex-shrink: 0;
      }

      .stat-info {
        flex: 1;
        min-width: 0;

        .stat-value {
          font-size: 36px;
          font-weight: 700;
          color: #333;
          margin-bottom: 8px;
          line-height: 1.2;
          
          .unit {
            font-size: 20px;
            font-weight: 500;
            margin-left: 4px;
          }
        }

        .stat-label {
          font-size: 14px;
          color: #666;
          white-space: nowrap;
        }
      }
    }
  }
}
</style>
