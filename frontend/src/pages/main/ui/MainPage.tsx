import { useEffect, useState } from 'react'
import { Button, FormField, IconButton, Input, Textarea } from '@shared'
import styles from './MainPage.module.css'

export function MainPage() {
  const [theme, setTheme] = useState<'dark' | 'light'>('dark')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])

  return (
    <main className={styles.page}>
      <section className={styles.demo}>
        <div className={styles.heading}>
          <div className={styles.headingTop}>
            <p className={styles.eyebrow}>Shared UI</p>
            <Button onClick={() => setTheme((currentTheme) => currentTheme === 'dark' ? 'light' : 'dark')} size="sm" variant="secondary">
              {theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}
            </Button>
          </div>
          <h1>Компоненты интерфейса</h1>
          <p>Черновая витрина для проверки состояний и размеров.</p>
        </div>

        <div className={styles.section}>
          <h2>Кнопки</h2>
          <div className={styles.row}>
            <Button>Сгенерировать</Button>
            <Button variant="secondary">Сохранить</Button>
            <Button variant="ghost">Отмена</Button>
            <Button variant="danger">Удалить</Button>
            <Button loading>Загрузка</Button>
          </div>
        </div>

        <div className={styles.section}>
          <h2>Кнопки с иконкой</h2>
          <div className={styles.row}>
            <IconButton aria-label="Добавить элемент">+</IconButton>
            <IconButton aria-label="Открыть меню" variant="ghost">•••</IconButton>
            <IconButton aria-label="Сохранить" variant="primary">↓</IconButton>
          </div>
        </div>

        <div className={styles.fields}>
          <FormField description="Используется в заголовке проекта." label="Название проекта" required>
            <Input defaultValue="Orion Labs" />
          </FormField>
          <FormField error="Введите корректный адрес электронной почты." label="Email">
            <Input invalid placeholder="name@company.com" type="email" />
          </FormField>
          <FormField label="Промпт для генерации">
            <Textarea placeholder="Например, современный технологичный логотип с градиентами…" rows={4} />
          </FormField>
        </div>
      </section>
    </main>
  )
}
