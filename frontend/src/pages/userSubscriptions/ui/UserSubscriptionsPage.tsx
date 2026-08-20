import { authStore } from '@app/providers/auth'
import {
  userProfileQueryKey,
  userSubscriptionsQueryKey,
  useUnsubscribeFromUserMutation,
  useUserSubscriptionsQuery,
} from '@entities/project'
import { getApiErrorMessage } from '@shared/api/api'
import { Button, useToast } from '@shared/ui'
import { useQueryClient } from '@tanstack/react-query'
import { UserConnections } from '@widgets'
import { useState } from 'react'
import { useParams } from 'react-router-dom'

export function UserSubscriptionsPage() {
  const { userId = '' } = useParams()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [pendingUserId, setPendingUserId] = useState<string | null>(null)
  const isOwnSubscriptions = authStore.getSession?.user.id === userId
  const {
    data: subscriptions = [],
    error,
    isError,
    isPending,
  } = useUserSubscriptionsQuery(userId)
  const {
    isPending: isUnsubscribingPending,
    mutateAsync: unsubscribeFromUser,
  } = useUnsubscribeFromUserMutation()

  async function handleUnfollow(userIdToUnfollow: string) {
    setPendingUserId(userIdToUnfollow)

    try {
      await unsubscribeFromUser(userIdToUnfollow)
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: userProfileQueryKey(userId),
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
      connections={subscriptions}
      emptyMessage="You are not following anyone yet."
      error={error}
      isError={isError}
      isPending={isPending}
      renderActions={
        isOwnSubscriptions
          ? (connection) => (
              <Button
                loading={
                  isUnsubscribingPending && pendingUserId === connection.id
                }
                onClick={() => void handleUnfollow(connection.id)}
                size="md"
                variant="outline"
              >
                Unfollow
              </Button>
            )
          : undefined
      }
      title="Your Following"
    />
  )
}
