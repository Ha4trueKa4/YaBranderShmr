import { Link } from 'react-router-dom'

import { AppRoute } from '@app/providers/router/routes'

import styles from './feedPage.module.css'

const projects = [
  { title: 'Команда продукта', author: 'Екатерина Петрова', palette: 'violet', initial: 'К', likes: 48 },
  { title: 'Летняя школа дизайна', author: 'Михаил Волков', palette: 'orange', initial: 'Ш', likes: 32 },
  { title: 'Frontend community', author: 'Алексей Смирнов', palette: 'blue', initial: 'F', likes: 67 },
  { title: 'Волонтёры фестиваля', author: 'Дарья Ковалёва', palette: 'green', initial: 'В', likes: 24 },
  { title: 'Creative meetup', author: 'Иван Лебедев', palette: 'pink', initial: 'C', likes: 39 },
  { title: 'Курс по продукту', author: 'Анна Орлова', palette: 'yellow', initial: 'П', likes: 56 },
]

export function FeedPage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <Link className={styles.brand} to={AppRoute.Feed}>
          <span className={styles.brandMark}>B</span>
          <span>Brandkit</span>
        </Link>
        <nav className={styles.navigation}>
          <Link className={styles.activeLink} to={AppRoute.Feed}>Лента</Link>
          <Link to={AppRoute.Profile}>Мои проекты</Link>
        </nav>
        <div className={styles.profileActions}>
          <Link className={styles.newProject} to={AppRoute.EditorCreate}>Создать проект</Link>
          <Link className={styles.avatar} to={AppRoute.Profile}>ЕК</Link>
        </div>
      </header>

      <main className={styles.content}>
        <section className={styles.intro}>
          <div>
            <p className={styles.overline}>Вдохновляйтесь работами сообщества</p>
            <h1>Лента проектов</h1>
            <p>Готовые образы для команд, сообществ и событий — сохраняйте идеи для своего следующего проекта.</p>
          </div>
          <label className={styles.search}>
            <span>⌕</span>
            <input placeholder="Поиск проектов" type="search" />
          </label>
        </section>

        <nav className={styles.filters}>
          <button className={styles.selectedFilter} type="button">Все работы</button>
          <button type="button">Команды</button>
          <button type="button">События</button>
          <button type="button">Сообщества</button>
        </nav>

        <section className={styles.grid}>
          {projects.map((project) => (
            <article className={styles.card} key={project.title}>
              <div className={`${styles.preview} ${styles[project.palette]}`}>
                <span className={styles.previewLabel}>BRANDKIT</span>
                <span className={styles.monogram}>{project.initial}</span>
                <span className={styles.orbit} />
              </div>
              <div className={styles.cardBody}>
                <div>
                  <h2>{project.title}</h2>
                  <p>{project.author}</p>
                </div>
                <span className={styles.likes}>♡ {project.likes}</span>
              </div>
            </article>
          ))}
        </section>
      </main>
    </div>
  )
}
