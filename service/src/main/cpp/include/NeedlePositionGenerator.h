#pragma once

#include <atomic>
#include <condition_variable>
#include <mutex>

#include <jni.h>

namespace generate_service
{

class NeedlePositionGenerator
{
public:
	static NeedlePositionGenerator & Instance();

	void set_enviroment(JNIEnv * jni_env, jobject generate_service, double max_generated_value);

	void start_generate();

	void stop_generate();

private:
	NeedlePositionGenerator() = default;

private:
	[[nodiscard]] JNIEnv * get_jni_env();

private:
	JavaVM * jvm_;
	jmethodID on_value_changed_;
	jobject object_ref_;

private:
	int init_value_{0};

	double max_generated_value_{0.0};

	std::atomic<bool> interrupt_generate_flag_{false};
	std::condition_variable condition_variable_;
	std::mutex mutex_;
};

} // namespace generate_service
