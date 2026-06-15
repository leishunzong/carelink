const api = require('../../../utils/api')

const SKILL_CATEGORIES = [
    { id: 0, name: '全部' },
    { id: 1, name: '临床医疗护理' },
    { id: 2, name: '基础生活照料' },
    { id: 3, name: '康复训练与介护' },
    { id: 4, name: '失智专项护理' },
    { id: 5, name: '居家安全与应急' },
    { id: 6, name: '精神慰藉与社交' }
]

const LEVEL_MAP = { 1: '初级', 2: '中级', 3: '高级' }
const AUDIT_STATUS_MAP = { 0: '待审核', 1: '已通过', 2: '已拒绝' }

Page({
    data: {
        categories: SKILL_CATEGORIES,
        activeCategory: 0,
        allSkills: [],
        skillList: [],
        loading: false
    },

    onLoad() {
        this.loadData()
    },

    onShow() {
        this.loadData()
    },

    // 调用 /caregiver/skill/list 接口，查询我的技能列表
    async loadData() {
        if (this.data.loading) return

        this.setData({ loading: true })

        try {
            const allSkills = (await api.skill.getMySkills()) || []

            // 补充显示文案
            allSkills.forEach(item => {
                item.levelText = LEVEL_MAP[item.level] || '初级'
                item.auditStatusText = AUDIT_STATUS_MAP[item.auditStatus] ?? '未知'
            })

            this.setData({ allSkills, loading: false })
            this.filterByCategory()
        } catch (err) {
            console.error('加载技能列表失败:', err)
            this.setData({ loading: false })
        }
    },

    // 按分类前端过滤
    filterByCategory() {
        const { allSkills, activeCategory } = this.data
        const skillList = activeCategory > 0
            ? allSkills.filter(item => item.skillType === activeCategory)
            : allSkills

        this.setData({ skillList })
    },

    // 切换分类
    onCategoryChange(e) {
        const id = parseInt(e.currentTarget.dataset.id)
        this.setData({ activeCategory: id })
        this.filterByCategory()
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadData()
        setTimeout(() => {
            wx.stopPullDownRefresh()
        }, 1000)
    },

    // 删除技能
    onDeleteSkill(e) {
        const { skillId, name } = e.currentTarget.dataset

        wx.showModal({
            title: '确认删除',
            content: `确定要删除技能「${name}」吗？删除后需重新申请。`,
            confirmColor: '#ee0a24',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        wx.showLoading({ title: '删除中...', mask: true })
                        await api.skill.delete(skillId)
                        wx.hideLoading()

                        wx.showToast({
                            title: '删除成功',
                            icon: 'success'
                        })

                        this.loadData()
                    } catch (err) {
                        wx.hideLoading()
                        console.error('删除技能失败:', err)
                    }
                }
            }
        })
    },

    // 跳转到申请技能页
    goToApplySkill() {
        wx.navigateTo({
            url: '/pages/service/skill-list/skill-list'
        })
    }
})
