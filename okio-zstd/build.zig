const std = @import("std");

pub fn build(b: *std.Build) !void {
  // The Windows builds create a .lib file in the lib/ directory which we don't need.
  const deleteLib = b.addRemoveDirTree(.{ .cwd_relative = b.getInstallPath(.prefix, "lib") });
  b.getInstallStep().dependOn(&deleteLib.step);

  try setupTarget(b, &deleteLib.step, .linux, .aarch64, "aarch64");
  try setupTarget(b, &deleteLib.step, .linux, .x86_64, "amd64");
  try setupTarget(b, &deleteLib.step, .macos, .aarch64, "aarch64");
  try setupTarget(b, &deleteLib.step, .macos, .x86_64, "x86_64");
}

fn setupTarget(b: *std.Build, step: *std.Build.Step, tag: std.Target.Os.Tag, arch: std.Target.Cpu.Arch, dir: []const u8) !void {
  const lib = b.addSharedLibrary(.{
    .name = "zstd",
    .target = b.resolveTargetQuery(.{
      .cpu_arch = arch,
      .os_tag = tag,
      // We need to explicitly specify gnu for linux, as otherwise it defaults to musl.
      // See https://github.com/ziglang/zig/issues/16624#issuecomment-1801175600.
      .abi = if (tag == .linux) .gnu else null,
    }),
    .optimize = .ReleaseSmall,
  });

  lib.addIncludePath(b.path("native/include/share"));
  lib.addIncludePath(
    switch (tag) {
      .windows => b.path("native/include/windows"),
      else => b.path("native/include/unix"),
    }
  );
  lib.addIncludePath(b.path("../zstd/lib"));

  lib.linkLibC();
  // TODO Tree-walk these two dirs for all C files.
  lib.addCSourceFiles(.{
    .files = &.{
      "../zstd/lib/common/debug.c",
      "../zstd/lib/common/entropy_common.c",
      "../zstd/lib/common/error_private.c",
      "../zstd/lib/common/fse_decompress.c",
      "../zstd/lib/common/pool.c",
      "../zstd/lib/common/threading.c",
      "../zstd/lib/common/xxhash.c",
      "../zstd/lib/common/zstd_common.c",
      "../zstd/lib/compress/fse_compress.c",
      "../zstd/lib/compress/hist.c",
      "../zstd/lib/compress/huf_compress.c",
      "../zstd/lib/compress/zstd_compress.c",
      "../zstd/lib/compress/zstd_compress_literals.c",
      "../zstd/lib/compress/zstd_compress_sequences.c",
      "../zstd/lib/compress/zstd_compress_superblock.c",
      "../zstd/lib/compress/zstd_double_fast.c",
      "../zstd/lib/compress/zstd_fast.c",
      "../zstd/lib/compress/zstd_lazy.c",
      "../zstd/lib/compress/zstd_ldm.c",
      "../zstd/lib/compress/zstd_opt.c",
      "../zstd/lib/compress/zstd_preSplit.c",
      "../zstd/lib/compress/zstdmt_compress.c",
      "../zstd/lib/decompress/huf_decompress.c",
      "../zstd/lib/decompress/huf_decompress_amd64.S",
      "../zstd/lib/decompress/zstd_ddict.c",
      "../zstd/lib/decompress/zstd_decompress.c",
      "../zstd/lib/decompress/zstd_decompress_block.c",
    },
    .flags = &.{
      "-std=gnu99",
    },
  });

  lib.linkLibCpp();
  // TODO Tree-walk this dirs for all C++ files.
  lib.addCSourceFiles(.{
    .files = &.{
      "native/OkioZstd.cpp",
    },
    .flags = &.{
      "-std=c++11",
    },
  });

  const install = b.addInstallArtifact(lib, .{
    .dest_dir = .{
      .override = .{
        .custom = dir,
      },
    },
  });

  step.dependOn(&install.step);
}
