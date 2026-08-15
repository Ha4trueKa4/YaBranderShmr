import {zodResolver} from '@hookform/resolvers/zod'
import {Button, FormField, Input, Portal} from '@shared/ui'
import {useEffect, useId} from 'react'
import {useForm} from 'react-hook-form'
import {z} from 'zod'

import styles from './ProjectRenameModal.module.css'

interface ProjectRenameModalProps {
    isOpen: boolean
    onClose: () => void
    onSubmit: (projectName: string) => void
    projectName: string
}

const projectRenameFormSchema = z.object({
    projectName: z.string().trim().nonempty('Enter a project name.'),
})

type ProjectRenameFormValues = z.infer<typeof projectRenameFormSchema>

export function ProjectRenameModal({isOpen, onClose, onSubmit, projectName}: ProjectRenameModalProps) {
    const projectNameInputId = useId()
    const form = useForm<ProjectRenameFormValues>({
        defaultValues: {projectName},
        mode: 'onSubmit',
        resolver: zodResolver(projectRenameFormSchema),
    })

    useEffect(() => {
        if (isOpen) {
            form.reset({projectName})
        }
    }, [form, isOpen, projectName])

    if (!isOpen) {
        return null
    }

    function handleClose() {
        form.reset({projectName})
        onClose()
    }

    const submit = form.handleSubmit(({projectName: nextProjectName}) => {
        onSubmit(nextProjectName.trim())
    })

    return (
        <Portal>
            <div className={styles.overlay}>
                <button
                    className={styles.backdrop}
                    onClick={handleClose}
                    type="button"
                />
                <section className={styles.modal}>
                    <div className={styles.header}>
                        <h2 className={styles.title}>Rename project</h2>
                    </div>
                    <form className={styles.form} onSubmit={submit}>
                        <FormField
                            error={form.formState.errors.projectName?.message}
                            htmlFor={projectNameInputId}
                            label="Project name"
                        >
                            <Input
                                {...form.register('projectName')}
                                autoFocus
                                id={projectNameInputId}
                                invalid={Boolean(form.formState.errors.projectName)}
                            />
                        </FormField>
                        <div className={styles.actions}>
                            <Button onClick={handleClose} variant="secondary">
                                Cancel
                            </Button>
                            <Button type="submit">Save name</Button>
                        </div>
                    </form>
                </section>
            </div>
        </Portal>
    )
}
