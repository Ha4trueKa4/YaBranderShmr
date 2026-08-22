cmake_minimum_required(VERSION 3.12)
project(ya_brander CXX)

set(CMAKE_POLICY_VERSION_MINIMUM 3.5)

include(FetchContent)

FetchContent_Declare(
        pugixml
        GIT_REPOSITORY https://github.com/zeux/pugixml.git
        GIT_TAG v1.14
)

FetchContent_Declare(
        nlohmann_json
        GIT_REPOSITORY https://github.com/nlohmann/json.git
        GIT_TAG v3.11.3
)

FetchContent_Declare(
        jwt-cpp
        GIT_REPOSITORY https://github.com/Thalhammer/jwt-cpp.git
        GIT_TAG v0.7.0
)

set(JSON_BuildTests OFF CACHE INTERNAL "")
set(JSON_Install OFF CACHE INTERNAL "")
set(JWT_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(pugixml)
FetchContent_MakeAvailable(nlohmann_json)
FetchContent_MakeAvailable(jwt-cpp)

# Adding userver dependency
find_package(userver COMPONENTS core postgresql otlp QUIET)
if (NOT userver_FOUND)  # Fallback to subdirectory usage
    # Enable userver libraries that are needed in this project
    set(USERVER_FEATURE_POSTGRESQL ON CACHE BOOL "" FORCE)

    # Compatibility mode: some systems don't support these features
    set(USERVER_FEATURE_CRYPTOPP_BLAKE2 OFF CACHE BOOL "" FORCE)
    set(USERVER_FEATURE_GRPC_CHANNELZ OFF CACHE BOOL "" FORCE)
    set(USERVER_FEATURE_REDIS_HI_MALLOC ON CACHE BOOL "" FORCE)

    if (EXISTS third_party/userver)
        message(STATUS "Using userver framework from third_party/userver")
        add_subdirectory(third_party/userver)
    else ()
        message(FATAL_ERROR "Either install the userver or provide a path to it")
    endif ()
endif ()

userver_setup_environment()


# Common sources
add_library(${PROJECT_NAME}_objs OBJECT
        src/handlers/check/check.hpp
        src/handlers/check/check.cpp
        src/handlers/auth/register_handler.cpp
        src/handlers/auth/register_handler.hpp
        src/handlers/auth/jwt_service.cpp
        src/handlers/auth/jwt_service.hpp
        src/handlers/auth/login_handler.hpp
        src/handlers/auth/login_handler.cpp
        src/handlers/auth/password_hasher.hpp
        src/handlers/auth/password_hasher.cpp
        src/handlers/auth/yandex_oauth_handler.hpp
        src/handlers/auth/yandex_oauth_handler.cpp
        src/handlers/auth/auth_method_handler.hpp
        src/handlers/auth/auth_method_handler.cpp
        src/handlers/auth/set_password_handler.hpp
        src/handlers/auth/set_password_handler.cpp
        src/components/auth_checker.hpp
        src/components/auth_checker.cpp
        src/components/project_manager_component.cpp
        src/handlers/projects/append_projects.cpp
        src/handlers/test_llm_handler/test_llm_handler.cpp
        src/handlers/test_llm_handler/test_llm_handler.hpp
        src/components/LLMClientComponent.hpp
        src/components/LLMClientComponent.cpp
        src/components/llm_prompt.hpp
        src/components/llm_models.hpp
        src/components/prompt_constructor/prompt_template.hpp
        src/components/prompt_constructor/prompt_template.cpp
        src/components/svg_validator.hpp
        src/components/svg_validator.cpp
        src/components/prompt_constructor/prompt_constructor_component.cpp
        src/components/vector_search_component.h
        src/handlers/projects/create_project.cpp
        src/handlers/projects/get_project.cpp
        src/handlers/projects/patch_project.cpp
        src/handlers/projects/svg_validation_error_response.hpp
        src/handlers/projects/delete_project.cpp
        src/handlers/projects/get_project_chat.cpp
        src/handlers/projects/generation/generate_project.cpp
        src/handlers/projects/generation/generate_status.cpp
        src/handlers/projects/generation/cancel_generation.cpp
        src/handlers/projects/generation/append_generation.cpp
        src/handlers/projects/versions/get_version.cpp
        src/handlers/projects/versions/restore_version.cpp
        src/handlers/projects/versions/list_versions.cpp
        src/handlers/projects/versions/append_versions.cpp
        src/components/version_storage/version_storage_component_impl.cpp
        src/handlers/users/append_users.hpp
        src/handlers/users/append_users.cpp
        src/handlers/users/subscription_handlers.hpp
        src/handlers/users/subscription_handlers.cpp
        src/handlers/get_models/get_models.hpp
        src/handlers/get_models/get_models.cpp
        src/components/vector_search_component.cpp
        src/handlers/test_vector_search/test_vector_search_handler.hpp
        src/handlers/test_vector_search/test_vector_search_handler.cpp
        src/handlers/feed/get_feed.h
        src/handlers/feed/get_feed.cpp
        src/components/object_storage/s3_object_storage_component.cpp
        src/components/subscription_storage/subscription_storage_component.cpp
        src/handlers/users/get_user_profile.cpp
        src/handlers/users/patch_user_profile.cpp
        src/handlers/users/user_subscriptions_handlers.cpp
        src/components/prompt_constructor/prompt_polisher_component.cpp
        src/handlers/test_llm_handler/test_polisher_handler.cpp
        src/utils/errors.hpp
        src/utils/errors.cpp
        src/components/garbage_collector/garbage_collector_component.cpp
)

target_link_libraries(${PROJECT_NAME}_objs PUBLIC
        userver::postgresql
        userver::otlp
        nlohmann_json::nlohmann_json
        jwt-cpp
        pugixml
)
target_include_directories(${PROJECT_NAME}_objs PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)

# The Service
add_executable(${PROJECT_NAME} src/main.cpp)
target_link_libraries(${PROJECT_NAME} PRIVATE ${PROJECT_NAME}_objs)

# Functional Tests
include(UserverTestsuite OPTIONAL)

userver_testsuite_add_simple()


# Install
include(GNUInstallDirs)

if (DEFINED ENV{PREFIX})
    message(STATUS "Set install prefix: $ENV{PREFIX}")
    file(TO_CMAKE_PATH "$ENV{PREFIX}" PREFIX_PATH)
    set(CMAKE_INSTALL_PREFIX ${PREFIX_PATH})
endif ()

file(GLOB CONFIGS_FILES ${CMAKE_CURRENT_SOURCE_DIR}/configs/*.yaml ${CMAKE_CURRENT_SOURCE_DIR}/configs/*.json)

install(TARGETS ${PROJECT_NAME} DESTINATION ${CMAKE_INSTALL_BINDIR} COMPONENT ${PROJECT_NAME})
install(FILES ${CONFIGS_FILES} DESTINATION ${CMAKE_INSTALL_SYSCONFDIR}/${PROJECT_NAME} COMPONENT ${PROJECT_NAME})
