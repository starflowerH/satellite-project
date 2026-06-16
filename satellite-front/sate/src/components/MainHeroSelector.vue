<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { HeroOption } from '@/api/hero'

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    heroes: HeroOption[]
    loading?: boolean
    limit?: number
    emptyText?: string
    searchPlaceholder?: string
  }>(),
  {
    loading: false,
    limit: 5,
    emptyText: '暂无可选英雄',
    searchPlaceholder: '搜索英雄名称或职业',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'limit-exceeded', limit: number): void
  (e: 'search-change', keyword: string): void
}>()

const keyword = ref('')

watch(keyword, (value) => {
  emit('search-change', value)
})

const selectedHeroes = computed(() => props.modelValue ?? [])

const filteredHeroes = computed(() => {
  const searchValue = keyword.value.trim().toLowerCase()
  if (!searchValue) {
    return props.heroes
  }

  return props.heroes.filter((hero) => {
    return [hero.name, hero.profession].some((text) => text.toLowerCase().includes(searchValue))
  })
})

const professionSections = computed(() => {
  const bucket = new Map<string, HeroOption[]>()

  for (const hero of filteredHeroes.value) {
    const sectionKey = (hero.profession || '').trim() || '未分类'
    if (!bucket.has(sectionKey)) {
      bucket.set(sectionKey, [])
    }
    bucket.get(sectionKey)!.push(hero)
  }

  return Array.from(bucket.entries())
    .sort((a, b) => a[0].localeCompare(b[0], 'zh-CN'))
    .map(([profession, heroes]) => ({ profession, heroes }))
})

const isSelected = (name: string) => {
  return selectedHeroes.value.includes(name)
}

const toggleHero = (name: string) => {
  const nextValue = [...selectedHeroes.value]

  if (nextValue.includes(name)) {
    emit(
      'update:modelValue',
      nextValue.filter((item) => item !== name),
    )
    return
  }

  if (nextValue.length >= props.limit) {
    emit('limit-exceeded', props.limit)
    return
  }

  emit('update:modelValue', [...nextValue, name])
}

const removeHero = (name: string) => {
  emit(
    'update:modelValue',
    selectedHeroes.value.filter((item) => item !== name),
  )
}
</script>

<template>
  <div class="hero-selector">
    <div class="selector-toolbar">
      <label class="search-box">
        <span class="search-icon">搜</span>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          :placeholder="searchPlaceholder"
        />
      </label>

      <div class="selected-meta">
        <span class="meta-label">已选</span>
        <strong>{{ selectedHeroes.length }}/{{ limit }}</strong>
      </div>
    </div>

    <div v-if="selectedHeroes.length" class="selected-list">
      <button
        v-for="hero in selectedHeroes"
        :key="hero"
        type="button"
        class="selected-chip"
        @click="removeHero(hero)"
      >
        <span>{{ hero }}</span>
        <span class="chip-remove">×</span>
      </button>
    </div>

    <div v-if="loading" class="state-card">英雄数据加载中...</div>
    <div v-else-if="!filteredHeroes.length" class="state-card">{{ emptyText }}</div>

    <div v-else class="profession-sections">
      <section
        v-for="section in professionSections"
        :key="section.profession"
        class="profession-section"
      >
        <header class="profession-header">
          <h4 class="profession-title">{{ section.profession }}</h4>
          <span class="profession-count">{{ section.heroes.length }}</span>
        </header>

        <div class="hero-grid">
          <button
            v-for="hero in section.heroes"
            :key="hero.heroId || hero.name"
            type="button"
            class="hero-card"
            :class="{ 'hero-card--active': isSelected(hero.name), 'hero-card--disabled': !isSelected(hero.name) && selectedHeroes.length >= limit }"
            @click="toggleHero(hero.name)"
          >
            <span class="hero-check">{{ isSelected(hero.name) ? '✓' : '' }}</span>
            <span class="hero-name">{{ hero.name }}</span>
            <span class="hero-profession">{{ hero.profession || '职业待定' }}</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hero-selector {
  display: grid;
  gap: 14px;
}

.selector-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(0, 229, 255, 0.18);
  background: rgba(7, 17, 32, 0.72);
}

.search-icon {
  color: rgba(148, 193, 236, 0.8);
  font-size: 14px;
  line-height: 1;
}

.search-input {
  width: 100%;
  border: none;
  background: transparent;
  color: #eef7ff;
  font-size: 14px;
}

.search-input:focus {
  outline: none;
}

.search-input::placeholder {
  color: rgba(148, 193, 236, 0.46);
}

.selected-meta {
  min-width: 76px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(0, 229, 255, 0.08);
  border: 1px solid rgba(0, 229, 255, 0.2);
  color: #e7f6ff;
  text-align: center;
}

.meta-label {
  display: block;
  margin-bottom: 2px;
  font-size: 12px;
  color: rgba(163, 207, 238, 0.75);
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(0, 229, 255, 0.25);
  background: rgba(0, 229, 255, 0.12);
  color: #dff9ff;
  cursor: pointer;
}

.chip-remove {
  font-size: 16px;
  line-height: 1;
}

.state-card {
  padding: 18px 16px;
  border-radius: 16px;
  border: 1px dashed rgba(130, 170, 212, 0.22);
  background: rgba(8, 17, 32, 0.5);
  color: rgba(187, 214, 240, 0.82);
  text-align: center;
}

.profession-sections {
  display: grid;
  gap: 14px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.profession-section {
  display: grid;
  gap: 10px;
}

.profession-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.profession-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(165, 204, 236, 0.9);
}

.profession-count {
  min-width: 26px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #d7f8ff;
  border: 1px solid rgba(0, 229, 255, 0.25);
  background: rgba(0, 229, 255, 0.1);
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.hero-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-height: 108px;
  padding: 16px 14px 14px;
  border-radius: 16px;
  border: 1px solid rgba(117, 164, 211, 0.16);
  background: linear-gradient(180deg, rgba(12, 23, 41, 0.92), rgba(8, 15, 28, 0.88));
  color: #edf7ff;
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);
}

.hero-card:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 229, 255, 0.32);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.22);
}

.hero-card--active {
  border-color: rgba(0, 229, 255, 0.62);
  box-shadow:
    0 0 0 1px rgba(0, 229, 255, 0.24),
    0 0 26px rgba(0, 229, 255, 0.14);
}

.hero-card--disabled {
  opacity: 0.58;
}

.hero-check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 229, 255, 0.22);
  background: rgba(255, 255, 255, 0.03);
  color: #0a101a;
  font-weight: 700;
}

.hero-card--active .hero-check {
  background: linear-gradient(135deg, #00e5ff, #00b8d4);
  color: #041018;
}

.hero-name {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
}

.hero-profession {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(174, 204, 232, 0.84);
  font-size: 12px;
}

@media (max-width: 640px) {
  .selector-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .selected-meta {
    width: 100%;
  }

  .hero-grid {
    grid-template-columns: 1fr;
  }
}
</style>
