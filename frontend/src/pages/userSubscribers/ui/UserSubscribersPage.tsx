import { authStore } from '@app/providers/auth'
import {
  userProfileQueryKey,
  userSubscribersQueryKey,
  userSubscriptionsQueryKey,
  useSubscribeToUserMutation,
  useUnsubscribeFromUserMutation,
  useUserSubscribersQuery,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { Button, useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { UserConnections } from '@widgets'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'

export function UserSubscribersPage() {
  const { t } = useTranslation()
  const { userId = '' } = useParams()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [pendingUserId, setPendingUserId] = useState<string | null>(null)
  const isOwnSubscribers = authStore.getSession?.user.id === userId
  const {
    data: subscribers = [],
    error,
    isError,
    isPending,
  } = useUserSubscribersQuery(userId)
  const { isPending: isSubscribingPending, mutateAsync: subscribeToUser } =
    useSubscribeToUserMutation()
  const {
    isPending: isUnsubscribingPending,
    mutateAsync: unsubscribeFromUser,
  } = useUnsubscribeFromUserMutation()

  async function handleFollow(connection: {
    id: string
    isSubscribed: boolean
  }) {
    setPendingUserId(connection.id)

    try {
      if (connection.isSubscribed) {
        await unsubscribeFromUser(connection.id)
      } else {
        await subscribeToUser(connection.id)
      }

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: userProfileQueryKey(userId),
        }),
        queryClient.invalidateQueries({
          queryKey: userProfileQueryKey(connection.id),
        }),
        queryClient.invalidateQueries({
          queryKey: userSubscribersQueryKey(userId),
        }),
        queryClient.invalidateQueries({
          queryKey: userSubscriptionsQueryKey(userId),
        }),
      ])
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setPendingUserId(null)
    }
  }

  return (
    <UserConnections
      connections={subscribers}
      emptyMessage={t('You have no followers yet.')}
      error={error}
      isError={isError}
      isPending={isPending}
      renderActions={
        isOwnSubscribers
          ? (connection) => (
              <Button
                loading={
                  (isSubscribingPending || isUnsubscribingPending) &&
                  pendingUserId === connection.id
                }
                onClick={() => void handleFollow(connection)}
                size="md"
                variant="outline"
              >
                {connection.isSubscribed ? t('Unfollow') : t('Follow')}
              </Button>
            )
          : undefined
      }
      title={t('Followers')}
    />
  )
}
