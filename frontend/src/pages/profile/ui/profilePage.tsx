import { AppRoute } from '@app/providers/router/routes'
import { Link } from 'react-router-dom'

import styles from './profilePage.module.css'

const projects = [
  {
    title: 'Новая команда',
    description: '6 вариантов · Черновик',
    tone: 'dark',
    initial: 'N',
  },
  {
    title: 'Design meetup',
    description: '12 вариантов · Опубликован',
    tone: 'lavender',
    initial: 'D',
  },
  {
    title: 'Команда Brandkit',
    description: '8 вариантов · Опубликован',
    tone: 'coral',
    initial: 'B',
  },
]

export function ProfilePage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link className={styles.brand} to={AppRoute.Feed}>
          <span>B</span>Brandkit
        </Link>
        <nav>
          <Link to={AppRoute.Feed}>Лента</Link>
          <Link className={styles.active} to={AppRoute.Profile}>
            Мои проекты
          </Link>
        </nav>
        <Link className={styles.headerAvatar} to={AppRoute.Profile}>
          ЕК
        </Link>
      </header>
      <main className={styles.content}>
        <section className={styles.profile}>
          <div className={styles.largeAvatar}>ЕК</div>
          <div>
            <h1>Екатерина Петрова</h1>
            <p>@ekaterina · Москва</p>
            <p className={styles.bio}>
              Создаю визуальные системы, которые помогают командам быть
              узнаваемыми.
            </p>
          </div>
          <button className={styles.edit} type="button">
            Редактировать профиль
          </button>
        </section>
        <section className={styles.projectsHeader}>
          <div>
            <h2>
              Мои проекты <span>3</span>
            </h2>
            <p>Все ваши сохранённые наборы и варианты</p>
          </div>
          <Link className={styles.create} to={AppRoute.EditorCreate}>
            + Создать проект
          </Link>
        </section>
        <section className={styles.grid}>
          {projects.map((project) => (
            <article className={styles.card} key={project.title}>
              <div className={`${styles.cover} ${styles[project.tone]}`}>
                <span>BRANDKIT</span>
                <strong>{project.initial}</strong>
              </div>
              <div className={styles.cardInfo}>
                <h3>{project.title}</h3>
                <p>{project.description}</p>
                <button type="button">•••</button>
              </div>
            </article>
          ))}
          <Link className={styles.emptyCard} to={AppRoute.EditorCreate}>
            <span>+</span>
            <strong>Создать новый проект</strong>
            <small>Начните с шаблона или чистого листа</small>
          </Link>
        </section>
      </main>
    </div>
  )
}
