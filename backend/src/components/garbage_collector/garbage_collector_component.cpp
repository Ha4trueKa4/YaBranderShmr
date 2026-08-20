#include "garbage_collector_component.hpp"

#include <cstdlib>
#include <exception>
#include <cstring>

#include <userver/components/component_context.hpp>
#include <userver/logging/log.hpp>
#include <userver/utils/periodic_task.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

namespace ya_brander::components {

GarbageCollectorComponent::GarbageCollectorComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      version_storage_(context.FindComponent<VersionStorageComponent>()),
      grace_(config["grace-seconds"].As<std::chrono::seconds>(std::chrono::seconds{3600})),
      max_orphan_ratio_(config["max-orphan-ratio"].As<double>(0.5)) {

  // GC активируется с помощью ENV VAR. По дефолту выключен:
  // только на проде ставить GC_ENABLED=true в .env.
  const char* gc_env = std::getenv("GC_ENABLED");
  const bool enabled =
      gc_env != nullptr &&
      (std::strcmp(gc_env, "true") == 0 || std::strcmp(gc_env, "1") == 0);
  if (!enabled) {
    // по дефолту неактивен.
    LOG_WARNING() << "garbage-collector is DISABLED (GC_ENABLED not set to "
                     "true). No sweeps will run. Set GC_ENABLED=true only where "
                     "the DB owns this bucket.";
    return;
  }

  const auto interval = config["interval-seconds"].As<std::chrono::seconds>(
      std::chrono::seconds{21600});

  userver::utils::PeriodicTask::Settings settings{interval};
  settings.flags = {userver::utils::PeriodicTask::Flags::kChaotic,
                    userver::utils::PeriodicTask::Flags::kNow};

  LOG_INFO() << "garbage-collector ENABLED: interval=" << interval.count()
             << "s grace=" << grace_.count()
             << "s max_orphan_ratio=" << max_orphan_ratio_;
  task_.Start("orphan-gc", settings, [this] { RunOnce(); });
}

GarbageCollectorComponent::~GarbageCollectorComponent() { task_.Stop(); }

void GarbageCollectorComponent::RunOnce() {
  try {
    const auto reclaimed = version_storage_.CollectGarbage(grace_, max_orphan_ratio_);
    LOG_INFO() << "orphan-gc run complete, reclaimed=" << reclaimed;
  } catch (const std::exception& e) {
    LOG_ERROR() << "orphan-gc run failed: " << e.what();
  }
}

userver::yaml_config::Schema GarbageCollectorComponent::GetStaticConfigSchema() {
  return userver::yaml_config::MergeSchemas<
      userver::components::LoggableComponentBase>(R"(
type: object
description: Background orphan garbage collector for object storage
additionalProperties: false
properties:
    interval-seconds:
        type: integer
        description: how often the sweep runs, seconds
        defaultDescription: '21600'
    grace-seconds:
        type: integer
        description: min object age to be eligible for deletion, seconds (race guard)
        defaultDescription: '3600'
    max-orphan-ratio:
        type: number
        description: abort the sweep if orphans exceed this fraction of the bucket (0..1)
        defaultDescription: '0.5'
)");
}

}  // namespace ya_brander::components