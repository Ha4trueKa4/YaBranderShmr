import {authStore} from '@app/providers/auth'
import {AppRoute} from '@app/providers/router/routes'
import {useProjectsQuery} from '@entities/project'
import {getApiErrorMessage} from '@shared/api/api'
import {Button, ErrorState} from '@shared/ui'
import {
    OwnProjectsSection,
    ProfileHeader,
    type ProfileHeaderProfile,
} from '@widgets'
import {Header} from '@widgets/header'
import {useNavigate} from 'react-router-dom'

import styles from './profilePage.module.css'

export function ProfilePage() {
    const navigate = useNavigate()
    const session = authStore.getSession

    if (!session) {
        return <ProfileUnavailable/>
    }

    const {data: projects = [], error: projectsError, isError: isProjectsError} = useProjectsQuery(session.user.id)

    const profile: ProfileHeaderProfile = {
        followersCount: 340,
        followingCount: 28,
        name: session.user.nickname,
        projectsCount: projects.length,
        username: session.user.nickname,
    }

    function handleLogout() {
        authStore.signOut()
        navigate(AppRoute.Auth, {replace: true, viewTransition: true})
    }

    return (
        <div className={styles.page}>
            <Header/>
            <main className={styles.content}>
                <ProfileHeader
                    actions={
                        <>
                            <Button onClick={handleLogout} variant="ghost">
                                Log out
                            </Button>
                            <Button variant="secondary">Edit profile</Button>
                        </>
                    }
                    profile={profile}
                />
                {isProjectsError ? (
                    <ErrorState message={getApiErrorMessage(projectsError)}/>
                ) : (
                    <OwnProjectsSection projects={projects}/>
                )}
            </main>
        </div>
    )
}

function ProfileUnavailable() {
    return (
        <div className={styles.page}>
            <Header/>
            <main className={styles.content}>
                <ErrorState message="Unable to load the profile. Please sign in and try again."/>
            </main>
        </div>
    )
}
