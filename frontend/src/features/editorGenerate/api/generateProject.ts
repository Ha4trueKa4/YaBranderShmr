import {api, parseApiResponse} from '@shared/api/api'
import {
    type PostProjectsByIdGenerateData,
    type PostProjectsByIdGenerateResponse,
    zPostProjectsByIdGenerateBody,
    zPostProjectsByIdGenerateResponse,
} from '@shared/api/types'

type GenerateProjectRequest = PostProjectsByIdGenerateData['body']

export async function generateProject(
    projectId: string,
    body: GenerateProjectRequest,
): Promise<PostProjectsByIdGenerateResponse> {
    const validBody = zPostProjectsByIdGenerateBody.parse(body)
    const response = await api<unknown, GenerateProjectRequest>(
        `/projects/${encodeURIComponent(projectId)}/generate`,
        {
            body: validBody,
            method: 'POST',
        },
    )

    console.log(response)

    return parseApiResponse(
        zPostProjectsByIdGenerateResponse,
        response,
        `/projects/${projectId}/generate`,
    )
}
