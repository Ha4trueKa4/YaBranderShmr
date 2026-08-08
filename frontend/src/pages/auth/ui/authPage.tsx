import { useHealthQuery } from '@shared/api/queries';

import styles from './authPage.module.css'

export function AuthPage() {
  const { data, error, isError, isPending } = useHealthQuery()

  return (
    <div className={styles.page}>
      <p>Страница Входа</p>
      <p>
        {isPending && 'Loading...'}
        {isError && `Сервер недоступен: ${error.message}`}
        {data && `Сервер доступен: ${data}`}
      </p>
    </div>
  )
}
