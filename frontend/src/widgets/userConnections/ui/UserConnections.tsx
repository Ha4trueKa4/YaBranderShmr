import { getApiErrorMessage } from '@shared/api/api'
import { getAvatarInitials } from '@shared/lib'
import { AsyncState, Avatar } from '@shared/ui'
import { Footer } from '@widgets/footer'
import { Header } from '@widgets/header'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import styles from './UserConnections.module.css'

interface UserConnection {
  id?: string
  is_subscribed?: boolean
  name?: string
}

interface ResolvedUserConnection {
  id: string
  isSubscribed: boolean
  name: string
}

interface UserConnectionsProps {
  connections: UserConnection[]
  emptyMessage: string
  error: unknown
  isError: boolean
  isPending: boolean
  renderActions?: (connection: ResolvedUserConnection) => ReactNode
  title: string
}

export function UserConnections({
  connections,
  emptyMessage,
  error,
  isError,
  isPending,
  renderActions,
  title,
}: UserConnectionsProps) {
  const validConnections = connections.flatMap((connection) => {
    if (!connection.id || !connection.name) {
      return []
    }

    return [
      {
        id: connection.id,
        isSubscribed: connection.is_subscribed ?? false,
        name: connection.name,
      },
    ]
  })

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <header className={styles.header}>
          <h1>{title}</h1>
        </header>
        <AsyncState
          errorMessage={getApiErrorMessage(error)}
          isError={isError}
          isPending={isPending}
        >
          {validConnections.length ? (
            <ul className={styles.list}>
              {validConnections.map((connection) => (
                <li className={styles.item} key={connection.id}>
                  <Link className={styles.user} to={`/users/${connection.id}`}>
                    <Avatar initials={getAvatarInitials(connection.name)} />
                    <span>
                      <strong>{connection.name}</strong>
                      <small>@{connection.name}</small>
                    </span>
                  </Link>
                  {renderActions && (
                    <div className={styles.itemActions}>
                      {renderActions(connection)}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className={styles.empty}>{emptyMessage}</p>
          )}
        </AsyncState>
      </main>
      <Footer />
    </div>
  )
}
